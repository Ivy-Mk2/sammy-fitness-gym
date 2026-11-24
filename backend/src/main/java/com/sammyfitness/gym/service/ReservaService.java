package com.sammyfitness.gym.service;

import com.sammyfitness.gym.dto.ReservaResponse;
import com.sammyfitness.gym.exception.ConflictoNegocioException;
import com.sammyfitness.gym.exception.RecursoNoEncontradoException;
import com.sammyfitness.gym.model.*;
import com.sammyfitness.gym.repository.ClaseRepository;
import com.sammyfitness.gym.repository.MembresiaRepository;
import com.sammyfitness.gym.repository.ReservaRepository;
import com.sammyfitness.gym.repository.UsuarioRepository;
import com.sammyfitness.gym.security.UsuarioPrincipal;
import com.sammyfitness.gym.service.correo.ReservaConfirmadaEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.sammyfitness.gym.service.ClaseService.ESTADOS_ACTIVOS;

/**
 * Reglas de reserva:
 * RN-01 solo reserva un cliente con membresía ACTIVA en la fecha de la clase.
 * RN-02 las reservas activas no superan el aforo (@Transactional + @Version en Clase → 409 si hay concurrencia).
 * RN-03 no se reserva dos veces la misma clase ni dos clases en el mismo horario.
 * RN-04 al cancelar se libera el cupo; no se cancelan clases ya realizadas.
 */
@Service
@RequiredArgsConstructor
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final ClaseRepository claseRepository;
    private final MembresiaRepository membresiaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ApplicationEventPublisher eventos;
    private final DtoMapper mapper;
    private final Clock clock;

    @Transactional
    public ReservaResponse reservar(Long clienteId, Long claseId) {
        Usuario cliente = usuarioRepository.findById(clienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("El cliente no existe"));
        // Incrementa @Version de la clase al confirmar: dos reservas simultáneas no pueden pasar ambas.
        Clase clase = claseRepository.findByIdConBloqueo(claseId)
                .orElseThrow(() -> new RecursoNoEncontradoException("La clase " + claseId + " no existe"));

        if (mapper.yaInicio(clase)) {
            throw new ConflictoNegocioException("La clase ya se realizó o está en curso");
        }
        // RN-01
        if (!membresiaRepository.tieneMembresiaActivaEn(clienteId, clase.getFecha())) {
            throw new ConflictoNegocioException("No tiene una membresía activa para la fecha de la clase");
        }
        // RN-03
        if (reservaRepository.existsByClienteIdAndClaseIdAndEstadoIn(clienteId, claseId, ESTADOS_ACTIVOS)) {
            throw new ConflictoNegocioException("Ya tiene una reserva para esta clase");
        }
        if (reservaRepository.existeCruceHorarioCliente(clienteId, ESTADOS_ACTIVOS, clase.getFecha(),
                clase.getHoraInicio(), clase.getHoraFin())) {
            throw new ConflictoNegocioException("Ya tiene otra clase reservada en el mismo horario");
        }
        // RN-02
        long ocupados = reservaRepository.countByClaseIdAndEstadoIn(claseId, ESTADOS_ACTIVOS);
        if (ocupados >= clase.getAforoMaximo()) {
            throw new ConflictoNegocioException("La clase no tiene cupos disponibles (aforo completo)");
        }

        Reserva reserva = reservaRepository.save(Reserva.builder()
                .cliente(cliente)
                .clase(clase)
                .estado(EstadoReserva.CONFIRMADA)
                .fechaReserva(LocalDateTime.now(clock))
                .build());

        // El correo se envía en otro hilo y solo si esta transacción se confirma.
        eventos.publishEvent(new ReservaConfirmadaEvent(cliente.getCorreo(), cliente.getNombreCompleto(),
                clase.getNombre(), clase.getEntrenador().getNombreCompleto(),
                clase.getFecha(), clase.getHoraInicio(), clase.getHoraFin()));

        return mapper.reserva(reserva);
    }

    @Transactional(readOnly = true)
    public List<ReservaResponse> misReservas(Long clienteId) {
        return reservaRepository.findDelCliente(clienteId).stream().map(mapper::reserva).toList();
    }

    /** RN-04: el cliente cancela su propia reserva; el cupo queda libre al pasar a CANCELADA. */
    @Transactional
    public ReservaResponse cancelar(Long clienteId, Long reservaId) {
        Reserva reserva = buscar(reservaId);
        if (!reserva.getCliente().getId().equals(clienteId)) {
            throw new AccessDeniedException("Solo puede cancelar sus propias reservas");
        }
        if (reserva.getEstado() != EstadoReserva.CONFIRMADA) {
            throw new ConflictoNegocioException("Solo se pueden cancelar reservas confirmadas (estado actual: "
                    + reserva.getEstado() + ")");
        }
        if (mapper.yaInicio(reserva.getClase())) {
            throw new ConflictoNegocioException("No se puede cancelar una clase que ya se realizó o está en curso");
        }
        reserva.setEstado(EstadoReserva.CANCELADA);
        return mapper.reserva(reserva);
    }

    /** Registro de asistencia (ADMINISTRADOR o el ENTRENADOR de la clase), desde el día de la clase. */
    @Transactional
    public ReservaResponse marcarAsistencia(UsuarioPrincipal actor, Long reservaId, boolean asistio) {
        Reserva reserva = buscar(reservaId);
        Clase clase = reserva.getClase();
        if (actor.rol() == Rol.ENTRENADOR && !clase.getEntrenador().getId().equals(actor.id())) {
            throw new AccessDeniedException("Solo puede registrar asistencia en sus propias clases");
        }
        if (reserva.getEstado() == EstadoReserva.CANCELADA) {
            throw new ConflictoNegocioException("La reserva está cancelada");
        }
        if (clase.getFecha().isAfter(LocalDate.now(clock))) {
            throw new ConflictoNegocioException("La asistencia se registra el día de la clase o después");
        }
        reserva.setEstado(asistio ? EstadoReserva.ASISTIDA : EstadoReserva.CONFIRMADA);
        return mapper.reserva(reserva);
    }

    private Reserva buscar(Long id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("La reserva " + id + " no existe"));
    }
}

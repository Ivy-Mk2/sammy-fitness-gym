package com.sammyfitness.gym.service;

import com.sammyfitness.gym.dto.*;
import com.sammyfitness.gym.exception.ConflictoNegocioException;
import com.sammyfitness.gym.exception.DatosInvalidosException;
import com.sammyfitness.gym.exception.RecursoNoEncontradoException;
import com.sammyfitness.gym.model.*;
import com.sammyfitness.gym.repository.MembresiaRepository;
import com.sammyfitness.gym.repository.PlanRepository;
import com.sammyfitness.gym.repository.UsuarioRepository;
import com.sammyfitness.gym.service.correo.NotificacionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Socios (clientes), planes y membresías.
 * - DNI y correo únicos (duplicado → 409).
 * - RN-05: fechaVencimiento = fechaInicio + duración del plan.
 * - RN-06: proceso diario de vencimientos y avisos.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClienteMembresiaService {

    private static final int LONGITUD_MINIMA_PASSWORD = 8;

    private final UsuarioRepository usuarioRepository;
    private final MembresiaRepository membresiaRepository;
    private final PlanRepository planRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificacionService notificaciones;
    private final DtoMapper mapper;
    private final Clock clock;

    @Value("${app.membresias.dias-aviso:3}")
    private int diasAviso = 3;

    // ------------------------------------------------------------------ Socios

    @Transactional(readOnly = true)
    public List<SocioResponse> listarSocios(String texto) {
        String filtro = texto == null || texto.isBlank() ? null : texto.trim();
        return usuarioRepository.buscarPorRol(Rol.CLIENTE, filtro).stream()
                .map(u -> mapper.socio(u, membresiaActual(u.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public SocioResponse obtenerSocio(Long id) {
        Usuario socio = buscarSocio(id);
        return mapper.socio(socio, membresiaActual(id));
    }

    @Transactional
    public SocioResponse crearSocio(SocioRequest req) {
        validarUnicos(req.dni(), req.correo(), null);
        if (req.password() == null || req.password().length() < LONGITUD_MINIMA_PASSWORD) {
            throw new DatosInvalidosException("password: debe tener al menos " + LONGITUD_MINIMA_PASSWORD + " caracteres");
        }
        Usuario socio = usuarioRepository.save(Usuario.builder()
                .nombres(req.nombres().trim())
                .apellidos(req.apellidos().trim())
                .dni(req.dni())
                .correo(req.correo().trim().toLowerCase())
                .telefono(req.telefono())
                .password(passwordEncoder.encode(req.password()))
                .rol(Rol.CLIENTE)
                .activo(req.activo() == null || req.activo())
                .build());
        return mapper.socio(socio, null);
    }

    @Transactional
    public SocioResponse actualizarSocio(Long id, SocioRequest req) {
        Usuario socio = buscarSocio(id);
        validarUnicos(req.dni(), req.correo(), id);

        socio.setNombres(req.nombres().trim());
        socio.setApellidos(req.apellidos().trim());
        socio.setDni(req.dni());
        socio.setCorreo(req.correo().trim().toLowerCase());
        socio.setTelefono(req.telefono());
        if (req.activo() != null) {
            socio.setActivo(req.activo());
        }
        if (req.password() != null && !req.password().isBlank()) {
            if (req.password().length() < LONGITUD_MINIMA_PASSWORD) {
                throw new DatosInvalidosException("password: debe tener al menos " + LONGITUD_MINIMA_PASSWORD + " caracteres");
            }
            socio.setPassword(passwordEncoder.encode(req.password()));
        }
        return mapper.socio(socio, membresiaActual(id));
    }

    // ------------------------------------------------------------------ Membresías

    /** RN-05: la fecha de vencimiento la calcula la entidad (fechaInicio + duracionDias). */
    @Transactional
    public MembresiaResponse asignarMembresia(Long socioId, MembresiaRequest req) {
        Usuario socio = buscarSocio(socioId);
        Plan plan = planRepository.findById(req.planId())
                .orElseThrow(() -> new RecursoNoEncontradoException("El plan " + req.planId() + " no existe"));
        LocalDate inicio = req.fechaInicio() != null ? req.fechaInicio() : LocalDate.now(clock);
        LocalDate fin = inicio.plusDays(plan.getDuracionDias());

        if (membresiaRepository.existeActivaEnPeriodo(socioId, inicio, fin)) {
            throw new ConflictoNegocioException("El socio ya tiene una membresía activa que se cruza con ese periodo");
        }
        Membresia membresia = membresiaRepository.save(Membresia.builder()
                .usuario(socio)
                .plan(plan)
                .fechaInicio(inicio)
                .estado(fin.isBefore(LocalDate.now(clock)) ? EstadoMembresia.VENCIDA : EstadoMembresia.ACTIVA)
                .build());
        return mapper.membresia(membresia);
    }

    @Transactional
    public MembresiaResponse cambiarEstado(Long membresiaId, EstadoMembresia nuevoEstado) {
        Membresia m = membresiaRepository.findById(membresiaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("La membresía " + membresiaId + " no existe"));
        if (nuevoEstado == EstadoMembresia.ACTIVA) {
            if (m.getFechaVencimiento().isBefore(LocalDate.now(clock))) {
                throw new ConflictoNegocioException("No se puede activar una membresía cuya fecha de vencimiento ya pasó");
            }
            if (m.getEstado() != EstadoMembresia.ACTIVA && membresiaRepository.existeActivaEnPeriodo(
                    m.getUsuario().getId(), m.getFechaInicio(), m.getFechaVencimiento())) {
                throw new ConflictoNegocioException("El socio ya tiene otra membresía activa en ese periodo");
            }
        }
        m.setEstado(nuevoEstado);
        return mapper.membresia(m);
    }

    /** Membresía del cliente autenticado: la vigente o, si no hay, la más reciente. */
    @Transactional(readOnly = true)
    public MembresiaResponse miMembresia(Long clienteId) {
        Membresia m = membresiaActual(clienteId);
        if (m == null) {
            throw new RecursoNoEncontradoException("No tiene membresías registradas");
        }
        return mapper.membresia(m);
    }

    @Transactional(readOnly = true)
    public List<PlanResponse> listarPlanes() {
        return planRepository.findAll(Sort.by("duracionDias")).stream().map(mapper::plan).toList();
    }

    // ------------------------------------------------------------------ RN-06

    /**
     * Marca VENCIDA las membresías ACTIVA cuya fecha de vencimiento ya pasó y envía un aviso
     * a las que vencen en {@code diasAviso} días o menos. Devuelve [vencidas, avisos].
     */
    @Transactional
    public int[] procesarVencimientos() {
        LocalDate hoy = LocalDate.now(clock);

        List<Membresia> expiradas = membresiaRepository.findByEstadoAndFechaVencimientoBefore(EstadoMembresia.ACTIVA, hoy);
        expiradas.forEach(m -> m.setEstado(EstadoMembresia.VENCIDA));

        List<Membresia> porVencer = membresiaRepository.findByEstadoAndFechaVencimientoBetween(
                EstadoMembresia.ACTIVA, hoy, hoy.plusDays(diasAviso));
        for (Membresia m : porVencer) {
            Usuario u = m.getUsuario();
            notificaciones.avisarVencimiento(u.getCorreo(), u.getNombreCompleto(), m.getPlan().getNombre(),
                    m.getFechaVencimiento(), ChronoUnit.DAYS.between(hoy, m.getFechaVencimiento()));
        }
        log.info("RN-06: {} membresías marcadas como VENCIDA, {} avisos de vencimiento enviados",
                expiradas.size(), porVencer.size());
        return new int[]{expiradas.size(), porVencer.size()};
    }

    // ------------------------------------------------------------------ Apoyo

    private Usuario buscarSocio(Long id) {
        return usuarioRepository.findByIdAndRol(id, Rol.CLIENTE)
                .orElseThrow(() -> new RecursoNoEncontradoException("El socio " + id + " no existe"));
    }

    private void validarUnicos(String dni, String correo, Long idActual) {
        boolean dniRepetido = idActual == null ? usuarioRepository.existsByDni(dni)
                : usuarioRepository.existsByDniAndIdNot(dni, idActual);
        if (dniRepetido) {
            throw new ConflictoNegocioException("Ya existe un usuario registrado con el DNI " + dni);
        }
        String correoNormalizado = correo.trim();
        boolean correoRepetido = idActual == null ? usuarioRepository.existsByCorreoIgnoreCase(correoNormalizado)
                : usuarioRepository.existsByCorreoIgnoreCaseAndIdNot(correoNormalizado, idActual);
        if (correoRepetido) {
            throw new ConflictoNegocioException("Ya existe un usuario registrado con el correo " + correoNormalizado);
        }
    }

    /** Vigente hoy (ACTIVA dentro de fechas); si no hay, la más reciente; null si nunca tuvo. */
    private Membresia membresiaActual(Long usuarioId) {
        List<Membresia> lista = membresiaRepository.findByUsuarioIdOrderByFechaInicioDesc(usuarioId);
        LocalDate hoy = LocalDate.now(clock);
        return lista.stream().filter(m -> m.estaVigenteEn(hoy)).findFirst()
                .orElse(lista.isEmpty() ? null : lista.get(0));
    }
}

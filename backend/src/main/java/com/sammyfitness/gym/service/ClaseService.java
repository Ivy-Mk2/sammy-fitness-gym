package com.sammyfitness.gym.service;

import com.sammyfitness.gym.dto.ClaseFiltro;
import com.sammyfitness.gym.dto.ClaseRequest;
import com.sammyfitness.gym.dto.ClaseResponse;
import com.sammyfitness.gym.dto.EntrenadorResponse;
import com.sammyfitness.gym.dto.InscritoResponse;
import com.sammyfitness.gym.exception.ConflictoNegocioException;
import com.sammyfitness.gym.exception.DatosInvalidosException;
import com.sammyfitness.gym.exception.RecursoNoEncontradoException;
import com.sammyfitness.gym.model.*;
import com.sammyfitness.gym.repository.ClaseRepository;
import com.sammyfitness.gym.repository.ReservaRepository;
import com.sammyfitness.gym.repository.UsuarioRepository;
import com.sammyfitness.gym.security.UsuarioPrincipal;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Reglas de clases:
 * - un entrenador no puede tener dos clases en el mismo horario;
 * - la fecha/hora no puede ser pasada y horaFin > horaInicio;
 * - el aforo no puede reducirse por debajo de las reservas activas;
 * - un ENTRENADOR solo gestiona sus propias clases (el ADMINISTRADOR, todas).
 */
@Service
@RequiredArgsConstructor
public class ClaseService {

    /** Reservas que ocupan cupo. ASISTIDA era una CONFIRMADA, así que también cuenta. */
    static final List<EstadoReserva> ESTADOS_ACTIVOS = List.of(EstadoReserva.CONFIRMADA, EstadoReserva.ASISTIDA);

    private final ClaseRepository claseRepository;
    private final ReservaRepository reservaRepository;
    private final UsuarioRepository usuarioRepository;
    private final DtoMapper mapper;
    private final Clock clock;

    // ------------------------------------------------------------------ Consultas

    @Transactional(readOnly = true)
    public List<ClaseResponse> listar(ClaseFiltro filtro) {
        List<Clase> clases = claseRepository.findAll(especificacion(filtro == null ? ClaseFiltro.vacio() : filtro),
                Sort.by("fecha", "horaInicio"));
        Map<Long, Long> ocupados = contarOcupados(clases.stream().map(Clase::getId).toList());
        return clases.stream()
                .map(c -> mapper.clase(c, ocupados.getOrDefault(c.getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public ClaseResponse obtener(Long id) {
        Clase clase = buscar(id);
        return mapper.clase(clase, reservaRepository.countByClaseIdAndEstadoIn(id, ESTADOS_ACTIVOS));
    }

    /** Lista de inscritos (reservas activas). Un ENTRENADOR solo ve los de sus clases. */
    @Transactional(readOnly = true)
    public List<InscritoResponse> inscritos(UsuarioPrincipal actor, Long claseId) {
        Clase clase = buscar(claseId);
        if (actor.rol() == Rol.ENTRENADOR) {
            verificarPropietario(actor, clase);
        }
        return reservaRepository.findByClaseIdAndEstadoInOrderByFechaReservaAsc(claseId, ESTADOS_ACTIVOS).stream()
                .map(mapper::inscrito)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EntrenadorResponse> listarEntrenadores() {
        return usuarioRepository.findByRol(Rol.ENTRENADOR).stream()
                .filter(Usuario::isActivo)
                .map(u -> new EntrenadorResponse(u.getId(), u.getNombreCompleto()))
                .sorted(Comparator.comparing(EntrenadorResponse::nombre))
                .toList();
    }

    // ------------------------------------------------------------------ Comandos

    @Transactional
    public ClaseResponse crear(UsuarioPrincipal actor, ClaseRequest req) {
        Usuario entrenador = resolverEntrenador(actor, req.entrenadorId());
        validarHorario(req, entrenador.getId(), null);

        Clase clase = Clase.builder()
                .nombre(req.nombre().trim())
                .disciplina(req.disciplina())
                .entrenador(entrenador)
                .fecha(req.fecha())
                .horaInicio(req.horaInicio())
                .horaFin(req.horaFin())
                .aforoMaximo(req.aforoMaximo())
                .descripcion(limpiar(req.descripcion()))
                .build();
        return mapper.clase(claseRepository.save(clase), 0);
    }

    @Transactional
    public ClaseResponse actualizar(UsuarioPrincipal actor, Long id, ClaseRequest req) {
        Clase clase = buscar(id);
        if (actor.rol() == Rol.ENTRENADOR) {
            verificarPropietario(actor, clase);
        }
        if (mapper.yaInicio(clase)) {
            throw new ConflictoNegocioException("No se puede modificar una clase que ya se realizó o está en curso");
        }

        Long entrenadorId = req.entrenadorId() != null ? req.entrenadorId() : clase.getEntrenador().getId();
        Usuario entrenador = resolverEntrenador(actor, entrenadorId);
        validarHorario(req, entrenador.getId(), id);

        long activos = reservaRepository.countByClaseIdAndEstadoIn(id, ESTADOS_ACTIVOS);
        if (req.aforoMaximo() < activos) {
            throw new ConflictoNegocioException("No se puede reducir el aforo a " + req.aforoMaximo()
                    + ": la clase ya tiene " + activos + " reservas confirmadas");
        }

        clase.setNombre(req.nombre().trim());
        clase.setDisciplina(req.disciplina());
        clase.setEntrenador(entrenador);
        clase.setFecha(req.fecha());
        clase.setHoraInicio(req.horaInicio());
        clase.setHoraFin(req.horaFin());
        clase.setAforoMaximo(req.aforoMaximo());
        clase.setDescripcion(limpiar(req.descripcion()));
        // flush para que un conflicto de @Version se detecte aquí y no al confirmar
        claseRepository.saveAndFlush(clase);
        return mapper.clase(clase, activos);
    }

    /** Solo se elimina si no tiene reservas activas (las canceladas se borran con la clase). */
    @Transactional
    public void eliminar(UsuarioPrincipal actor, Long id) {
        Clase clase = buscar(id);
        if (actor.rol() == Rol.ENTRENADOR) {
            verificarPropietario(actor, clase);
        }
        long activos = reservaRepository.countByClaseIdAndEstadoIn(id, ESTADOS_ACTIVOS);
        if (activos > 0) {
            throw new ConflictoNegocioException("No se puede eliminar la clase: tiene " + activos
                    + " reservas confirmadas o asistidas");
        }
        reservaRepository.eliminarPorClase(id);
        claseRepository.delete(clase);
    }

    // ------------------------------------------------------------------ Apoyo

    Clase buscar(Long id) {
        return claseRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("La clase " + id + " no existe"));
    }

    private Usuario resolverEntrenador(UsuarioPrincipal actor, Long entrenadorId) {
        if (actor.rol() == Rol.ENTRENADOR) {
            if (entrenadorId != null && !entrenadorId.equals(actor.id())) {
                throw new AccessDeniedException("Un entrenador solo puede gestionar sus propias clases");
            }
            entrenadorId = actor.id();
        } else if (entrenadorId == null) {
            throw new DatosInvalidosException("entrenadorId: es obligatorio");
        }
        Usuario entrenador = usuarioRepository.findByIdAndRol(entrenadorId, Rol.ENTRENADOR)
                .orElseThrow(() -> new DatosInvalidosException("El entrenador indicado no existe"));
        if (!entrenador.isActivo()) {
            throw new DatosInvalidosException("El entrenador indicado está inactivo");
        }
        return entrenador;
    }

    private void validarHorario(ClaseRequest req, Long entrenadorId, Long excluirClaseId) {
        if (!req.horaFin().isAfter(req.horaInicio())) {
            throw new DatosInvalidosException("La hora de fin debe ser posterior a la hora de inicio");
        }
        if (!LocalDateTime.of(req.fecha(), req.horaInicio()).isAfter(LocalDateTime.now(clock))) {
            throw new DatosInvalidosException("La fecha y hora de la clase no pueden ser pasadas");
        }
        if (claseRepository.existeCruceEntrenador(entrenadorId, req.fecha(), req.horaInicio(), req.horaFin(), excluirClaseId)) {
            throw new ConflictoNegocioException("El entrenador ya tiene otra clase en ese horario");
        }
    }

    private void verificarPropietario(UsuarioPrincipal actor, Clase clase) {
        if (!clase.getEntrenador().getId().equals(actor.id())) {
            throw new AccessDeniedException("Un entrenador solo puede gestionar sus propias clases");
        }
    }

    private Map<Long, Long> contarOcupados(Collection<Long> claseIds) {
        if (claseIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> resultado = new HashMap<>();
        for (Object[] fila : reservaRepository.contarPorClases(claseIds, ESTADOS_ACTIVOS)) {
            resultado.put((Long) fila[0], (Long) fila[1]);
        }
        return resultado;
    }

    private Specification<Clase> especificacion(ClaseFiltro f) {
        LocalDate hoy = LocalDate.now(clock);
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (f.disciplina() != null) {
                p.add(cb.equal(root.get("disciplina"), f.disciplina()));
            }
            if (f.dia() != null) {
                p.add(cb.equal(root.get("fecha"), f.dia()));
            } else {
                p.add(cb.greaterThanOrEqualTo(root.get("fecha"), hoy));
            }
            if (f.entrenadorId() != null) {
                p.add(cb.equal(root.get("entrenador").get("id"), f.entrenadorId()));
            }
            if (f.horario() != null) {
                p.add(cb.greaterThanOrEqualTo(root.get("horaInicio"), f.horario().getDesde()));
                if (f.horario().getHasta() != null) {
                    p.add(cb.lessThan(root.get("horaInicio"), f.horario().getHasta()));
                }
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
    }

    private static String limpiar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}

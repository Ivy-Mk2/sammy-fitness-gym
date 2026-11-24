package com.sammyfitness.gym.service;

import com.sammyfitness.gym.dto.*;
import com.sammyfitness.gym.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/** Convierte entidades en DTO: la API nunca expone entidades JPA. */
@Component
@RequiredArgsConstructor
public class DtoMapper {

    private final Clock clock;

    public ClaseResponse clase(Clase c, long ocupados) {
        Usuario e = c.getEntrenador();
        return new ClaseResponse(c.getId(), c.getNombre(), c.getDisciplina(),
                e.getId(), e.getNombreCompleto(),
                c.getFecha(), c.getHoraInicio(), c.getHoraFin(), c.getAforoMaximo(),
                ocupados, Math.max(0, c.getAforoMaximo() - ocupados), c.getDescripcion());
    }

    public ReservaResponse reserva(Reserva r) {
        Clase c = r.getClase();
        boolean cancelable = r.getEstado() == EstadoReserva.CONFIRMADA && !yaInicio(c);
        return new ReservaResponse(r.getId(), c.getId(), c.getNombre(), c.getDisciplina(),
                c.getEntrenador().getNombreCompleto(), c.getFecha(), c.getHoraInicio(), c.getHoraFin(),
                r.getEstado(), r.getFechaReserva(), cancelable);
    }

    public InscritoResponse inscrito(Reserva r) {
        Usuario u = r.getCliente();
        return new InscritoResponse(r.getId(), u.getId(), u.getNombreCompleto(), u.getDni(),
                u.getTelefono(), u.getCorreo(), r.getEstado(), r.getFechaReserva());
    }

    public MembresiaResponse membresia(Membresia m) {
        long dias = Math.max(0, ChronoUnit.DAYS.between(LocalDate.now(clock), m.getFechaVencimiento()));
        Plan p = m.getPlan();
        return new MembresiaResponse(m.getId(), m.getUsuario().getId(), p.getId(), p.getNombre(), p.getPrecio(),
                m.getFechaInicio(), m.getFechaVencimiento(), m.getEstado(), dias);
    }

    public SocioResponse socio(Usuario u, Membresia actual) {
        return new SocioResponse(u.getId(), u.getNombres(), u.getApellidos(), u.getDni(), u.getCorreo(),
                u.getTelefono(), u.isActivo(), actual == null ? null : membresia(actual));
    }

    public PlanResponse plan(Plan p) {
        return new PlanResponse(p.getId(), p.getNombre(), p.getDuracionDias(), p.getPrecio());
    }

    /** true si la clase ya empezó (o terminó) según la hora del gimnasio. */
    public boolean yaInicio(Clase c) {
        return !LocalDateTime.of(c.getFecha(), c.getHoraInicio()).isAfter(LocalDateTime.now(clock));
    }
}

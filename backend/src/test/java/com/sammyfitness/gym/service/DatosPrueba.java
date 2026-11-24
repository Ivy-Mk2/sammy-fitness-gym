package com.sammyfitness.gym.service;

import com.sammyfitness.gym.model.*;
import com.sammyfitness.gym.repository.*;
import com.sammyfitness.gym.security.UsuarioPrincipal;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicInteger;

/** Crea datos aislados para las pruebas (DNI y correos únicos en cada llamada). */
@Component
public class DatosPrueba {

    private static final AtomicInteger SECUENCIA = new AtomicInteger(80_000_000);

    private final UsuarioRepository usuarios;
    private final PlanRepository planes;
    private final MembresiaRepository membresias;
    private final ClaseRepository clases;
    private final ReservaRepository reservas;

    public DatosPrueba(UsuarioRepository usuarios, PlanRepository planes, MembresiaRepository membresias,
                       ClaseRepository clases, ReservaRepository reservas) {
        this.usuarios = usuarios;
        this.planes = planes;
        this.membresias = membresias;
        this.clases = clases;
        this.reservas = reservas;
    }

    public Usuario usuario(Rol rol) {
        int n = SECUENCIA.incrementAndGet();
        return usuarios.save(Usuario.builder()
                .nombres("Prueba").apellidos("Usuario " + n)
                .dni(String.valueOf(n)).correo("prueba" + n + "@correo.test").telefono("999999999")
                .password("$2a$10$hash").rol(rol).activo(true).build());
    }

    public Plan planMensual() {
        return planes.findByNombreIgnoreCase("Mensual").orElseGet(() ->
                planes.save(Plan.builder().nombre("Mensual").duracionDias(30).precio(new BigDecimal("120.00")).build()));
    }

    public Membresia membresia(Usuario cliente, LocalDate inicio, EstadoMembresia estado) {
        return membresias.save(Membresia.builder()
                .usuario(cliente).plan(planMensual()).fechaInicio(inicio).estado(estado).build());
    }

    /** Cliente con membresía mensual ACTIVA que empezó hoy. */
    public Usuario clienteConMembresia() {
        Usuario cliente = usuario(Rol.CLIENTE);
        membresia(cliente, LocalDate.now(), EstadoMembresia.ACTIVA);
        return cliente;
    }

    public Clase clase(Usuario entrenador, LocalDate fecha, String inicio, String fin, int aforo) {
        return clases.save(Clase.builder()
                .nombre("Clase de prueba").disciplina(Disciplina.FUNCIONAL).entrenador(entrenador)
                .fecha(fecha).horaInicio(LocalTime.parse(inicio)).horaFin(LocalTime.parse(fin))
                .aforoMaximo(aforo).build());
    }

    public Reserva reserva(Usuario cliente, Clase clase, EstadoReserva estado) {
        return reservas.save(Reserva.builder().cliente(cliente).clase(clase).estado(estado).build());
    }

    public static UsuarioPrincipal principal(Usuario u) {
        return UsuarioPrincipal.desde(u);
    }
}

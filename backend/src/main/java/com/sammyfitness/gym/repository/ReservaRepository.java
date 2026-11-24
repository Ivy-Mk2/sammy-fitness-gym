package com.sammyfitness.gym.repository;

import com.sammyfitness.gym.model.EstadoReserva;
import com.sammyfitness.gym.model.Reserva;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    long countByClaseIdAndEstado(Long claseId, EstadoReserva estado);

    long countByClaseIdAndEstadoIn(Long claseId, Collection<EstadoReserva> estados);

    @EntityGraph(attributePaths = "cliente")
    List<Reserva> findByClaseIdAndEstadoInOrderByFechaReservaAsc(Long claseId, Collection<EstadoReserva> estados);

    List<Reserva> findByClaseIdAndEstadoIn(Long claseId, List<EstadoReserva> estados);

    List<Reserva> findByClienteIdOrderByFechaReservaDesc(Long clienteId);

    @EntityGraph(attributePaths = {"clase", "clase.entrenador"})
    @Query("""
            select r from Reserva r
            where r.cliente.id = :clienteId
            order by r.clase.fecha desc, r.clase.horaInicio desc
            """)
    List<Reserva> findDelCliente(@Param("clienteId") Long clienteId);

    boolean existsByClienteIdAndClaseIdAndEstadoIn(Long clienteId, Long claseId, Collection<EstadoReserva> estados);

    /** RN-03: ¿el cliente ya tiene una reserva activa en otra clase que se cruce en horario? */
    @Query("""
            select count(r) > 0 from Reserva r
            where r.cliente.id = :clienteId
              and r.estado in :estados
              and r.clase.fecha = :fecha
              and r.clase.horaInicio < :fin
              and r.clase.horaFin > :inicio
            """)
    boolean existeCruceHorarioCliente(@Param("clienteId") Long clienteId,
                                      @Param("estados") Collection<EstadoReserva> estados,
                                      @Param("fecha") LocalDate fecha,
                                      @Param("inicio") LocalTime inicio,
                                      @Param("fin") LocalTime fin);

    /** Cupos ocupados por clase: filas [claseId, cantidad]. */
    @Query("""
            select r.clase.id, count(r) from Reserva r
            where r.clase.id in :claseIds and r.estado in :estados
            group by r.clase.id
            """)
    List<Object[]> contarPorClases(@Param("claseIds") Collection<Long> claseIds,
                                   @Param("estados") Collection<EstadoReserva> estados);

    @Modifying
    @Query("delete from Reserva r where r.clase.id = :claseId")
    void eliminarPorClase(@Param("claseId") Long claseId);
}

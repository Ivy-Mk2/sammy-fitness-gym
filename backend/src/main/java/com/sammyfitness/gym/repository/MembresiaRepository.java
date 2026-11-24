package com.sammyfitness.gym.repository;

import com.sammyfitness.gym.model.EstadoMembresia;
import com.sammyfitness.gym.model.Membresia;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface MembresiaRepository extends JpaRepository<Membresia, Long> {

    @EntityGraph(attributePaths = "plan")
    List<Membresia> findByUsuarioIdOrderByFechaInicioDesc(Long usuarioId);

    @EntityGraph(attributePaths = {"usuario", "plan"})
    List<Membresia> findByEstadoAndFechaVencimientoBefore(EstadoMembresia estado, LocalDate fecha);

    @EntityGraph(attributePaths = {"usuario", "plan"})
    List<Membresia> findByEstadoAndFechaVencimientoBetween(EstadoMembresia estado, LocalDate desde, LocalDate hasta);

    /** RN-01: ¿el usuario tiene una membresía ACTIVA que cubra la fecha dada? */
    @Query("""
            select count(m) > 0 from Membresia m
            where m.usuario.id = :usuarioId
              and m.estado = com.sammyfitness.gym.model.EstadoMembresia.ACTIVA
              and m.fechaInicio <= :fecha
              and m.fechaVencimiento >= :fecha
            """)
    boolean tieneMembresiaActivaEn(@Param("usuarioId") Long usuarioId, @Param("fecha") LocalDate fecha);

    /** ¿Existe una membresía ACTIVA del usuario que se cruce con el periodo [inicio, fin]? */
    @Query("""
            select count(m) > 0 from Membresia m
            where m.usuario.id = :usuarioId
              and m.estado = com.sammyfitness.gym.model.EstadoMembresia.ACTIVA
              and m.fechaInicio <= :fin
              and m.fechaVencimiento >= :inicio
            """)
    boolean existeActivaEnPeriodo(@Param("usuarioId") Long usuarioId,
                                  @Param("inicio") LocalDate inicio,
                                  @Param("fin") LocalDate fin);
}

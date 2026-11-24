package com.sammyfitness.gym.repository;

import com.sammyfitness.gym.model.Clase;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface ClaseRepository extends JpaRepository<Clase, Long>, JpaSpecificationExecutor<Clase> {

    List<Clase> findByEntrenadorIdAndFecha(Long entrenadorId, LocalDate fecha);

    List<Clase> findByFechaGreaterThanEqualOrderByFechaAscHoraInicioAsc(LocalDate fecha);

    /** Listado con filtros, cargando el entrenador en la misma consulta. */
    @Override
    @EntityGraph(attributePaths = "entrenador")
    List<Clase> findAll(Specification<Clase> spec, Sort sort);

    /**
     * RN-02: carga la clase y fuerza el incremento de @Version al confirmar la transacción.
     * Si dos reservas concurrentes leen la misma versión, solo una puede confirmar; la otra
     * recibe una excepción de bloqueo optimista (→ 409).
     */
    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Query("select c from Clase c where c.id = :id")
    Optional<Clase> findByIdConBloqueo(@Param("id") Long id);

    /** ¿El entrenador tiene otra clase que se cruce en fecha y horario? */
    @Query("""
            select count(c) > 0 from Clase c
            where c.entrenador.id = :entrenadorId
              and c.fecha = :fecha
              and c.horaInicio < :fin
              and c.horaFin > :inicio
              and (:excluirId is null or c.id <> :excluirId)
            """)
    boolean existeCruceEntrenador(@Param("entrenadorId") Long entrenadorId,
                                  @Param("fecha") LocalDate fecha,
                                  @Param("inicio") LocalTime inicio,
                                  @Param("fin") LocalTime fin,
                                  @Param("excluirId") Long excluirId);
}

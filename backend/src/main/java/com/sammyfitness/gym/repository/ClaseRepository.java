package com.sammyfitness.gym.repository;

import com.sammyfitness.gym.model.Clase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ClaseRepository extends JpaRepository<Clase, Long> {

    List<Clase> findByEntrenadorIdAndFecha(Long entrenadorId, LocalDate fecha);

    List<Clase> findByFechaGreaterThanEqualOrderByFechaAscHoraInicioAsc(LocalDate fecha);
}

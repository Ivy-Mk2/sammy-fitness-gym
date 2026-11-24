package com.sammyfitness.gym.repository;

import com.sammyfitness.gym.model.EstadoMembresia;
import com.sammyfitness.gym.model.Membresia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface MembresiaRepository extends JpaRepository<Membresia, Long> {

    List<Membresia> findByUsuarioIdOrderByFechaInicioDesc(Long usuarioId);

    List<Membresia> findByEstadoAndFechaVencimientoBefore(EstadoMembresia estado, LocalDate fecha);

    List<Membresia> findByEstadoAndFechaVencimientoBetween(EstadoMembresia estado, LocalDate desde, LocalDate hasta);
}

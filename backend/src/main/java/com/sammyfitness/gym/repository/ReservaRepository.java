package com.sammyfitness.gym.repository;

import com.sammyfitness.gym.model.EstadoReserva;
import com.sammyfitness.gym.model.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    long countByClaseIdAndEstado(Long claseId, EstadoReserva estado);

    List<Reserva> findByClaseIdAndEstadoIn(Long claseId, List<EstadoReserva> estados);

    List<Reserva> findByClienteIdOrderByFechaReservaDesc(Long clienteId);
}

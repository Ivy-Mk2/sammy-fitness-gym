package com.sammyfitness.gym.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "reservas",
        indexes = {
                @Index(name = "idx_reserva_clase_estado", columnList = "clase_id, estado"),
                @Index(name = "idx_reserva_cliente", columnList = "cliente_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"cliente", "clase"})
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Usuario cliente;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clase_id", nullable = false)
    private Clase clase;

    @Column(name = "fecha_reserva", nullable = false)
    private LocalDateTime fechaReserva;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoReserva estado = EstadoReserva.CONFIRMADA;

    @PrePersist
    void prePersist() {
        if (fechaReserva == null) {
            fechaReserva = LocalDateTime.now();
        }
    }
}

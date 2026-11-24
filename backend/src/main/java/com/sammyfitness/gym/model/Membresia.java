package com.sammyfitness.gym.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "membresias")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"usuario", "plan"})
public class Membresia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @NotNull
    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    /** RN-05: siempre = fechaInicio + plan.duracionDias (se recalcula antes de guardar). */
    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoMembresia estado = EstadoMembresia.ACTIVA;

    @PrePersist
    @PreUpdate
    public void calcularFechaVencimiento() {
        if (fechaInicio != null && plan != null && plan.getDuracionDias() != null) {
            this.fechaVencimiento = fechaInicio.plusDays(plan.getDuracionDias());
        }
    }

    /** true si la membresía está ACTIVA y la fecha dada cae dentro de su vigencia. */
    public boolean estaVigenteEn(LocalDate fecha) {
        return estado == EstadoMembresia.ACTIVA
                && fecha != null
                && fechaInicio != null
                && fechaVencimiento != null
                && !fecha.isBefore(fechaInicio)
                && !fecha.isAfter(fechaVencimiento);
    }
}

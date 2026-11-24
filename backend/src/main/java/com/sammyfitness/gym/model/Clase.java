package com.sammyfitness.gym.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "clases",
        indexes = @Index(name = "idx_clase_fecha", columnList = "fecha"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "entrenador")
public class Clase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(min = 3, max = 60)
    @Column(nullable = false, length = 60)
    private String nombre;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Disciplina disciplina;

    /** Usuario con rol ENTRENADOR (se valida en ClaseService). */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entrenador_id", nullable = false)
    private Usuario entrenador;

    @NotNull
    @Column(nullable = false)
    private LocalDate fecha;

    @NotNull
    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @NotNull
    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @NotNull
    @Min(1)
    @Max(50)
    @Column(name = "aforo_maximo", nullable = false)
    private Integer aforoMaximo;

    @Size(max = 200)
    @Column(length = 200)
    private String descripcion;

    /** Bloqueo optimista (RN-02). */
    @Version
    private Long version;
}

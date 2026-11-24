package com.sammyfitness.gym.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "usuarios",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_usuario_dni", columnNames = "dni"),
                @UniqueConstraint(name = "uk_usuario_correo", columnNames = "correo")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "password")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 80)
    @Column(nullable = false, length = 80)
    private String nombres;

    @NotBlank
    @Size(max = 80)
    @Column(nullable = false, length = 80)
    private String apellidos;

    @NotBlank
    @Pattern(regexp = "\\d{8}", message = "El DNI debe tener 8 dígitos")
    @Column(nullable = false, length = 8)
    private String dni;

    @NotBlank
    @Email
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String correo;

    @NotBlank
    @Pattern(regexp = "\\d{9}", message = "El teléfono debe tener 9 dígitos")
    @Column(nullable = false, length = 9)
    private String telefono;

    /** Hash BCrypt; nunca la contraseña en texto plano. */
    @NotBlank
    @Column(nullable = false, length = 100)
    private String password;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol;

    @Builder.Default
    @Column(nullable = false)
    private boolean activo = true;

    public String getNombreCompleto() {
        return nombres + " " + apellidos;
    }
}

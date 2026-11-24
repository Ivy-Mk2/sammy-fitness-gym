package com.sammyfitness.gym.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Alta y edición de socio (cliente).
 * password: obligatoria al crear (8-100 caracteres); al editar es opcional (vacía o ausente = no se cambia).
 * activo: solo se usa al editar (null = no se cambia).
 */
public record SocioRequest(
        @NotBlank(message = "Los nombres son obligatorios")
        @Size(max = 80, message = "Los nombres admiten máximo 80 caracteres")
        String nombres,

        @NotBlank(message = "Los apellidos son obligatorios")
        @Size(max = 80, message = "Los apellidos admiten máximo 80 caracteres")
        String apellidos,

        @NotBlank(message = "El DNI es obligatorio")
        @Pattern(regexp = "\\d{8}", message = "El DNI debe tener exactamente 8 dígitos")
        String dni,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 120, message = "El correo admite máximo 120 caracteres")
        String correo,

        @NotBlank(message = "El teléfono es obligatorio")
        @Pattern(regexp = "\\d{9}", message = "El teléfono debe tener exactamente 9 dígitos")
        String telefono,

        /* Vacía solo se acepta al editar (= no cambiar); al crear el servicio la exige. */
        @Pattern(regexp = "^$|^.{8,100}$", message = "La contraseña debe tener entre 8 y 100 caracteres")
        String password,

        Boolean activo
) {
}

package com.sammyfitness.gym.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 120)
        String correo,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(max = 100)
        String password
) {
}

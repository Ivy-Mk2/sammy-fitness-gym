package com.sammyfitness.gym.dto;

import com.sammyfitness.gym.model.Rol;

public record LoginResponse(String token, Rol rol, String nombre) {
}

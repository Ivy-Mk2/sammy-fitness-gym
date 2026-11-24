package com.sammyfitness.gym.dto;

import java.math.BigDecimal;

public record PlanResponse(Long id, String nombre, int duracionDias, BigDecimal precio) {
}

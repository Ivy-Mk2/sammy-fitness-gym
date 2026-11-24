package com.sammyfitness.gym.controller;

import com.sammyfitness.gym.dto.EntrenadorResponse;
import com.sammyfitness.gym.service.ClaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "7. Entrenadores", description = "Entrenadores activos (para filtros y formularios)")
@RestController
@RequestMapping("/api/entrenadores")
@RequiredArgsConstructor
public class EntrenadorController {

    private final ClaseService claseService;

    @Operation(summary = "Listar entrenadores",
            description = "Todos los autenticados. Solo id y nombre de los entrenadores activos, ordenados por nombre.")
    @GetMapping
    public List<EntrenadorResponse> listar() {
        return claseService.listarEntrenadores();
    }
}

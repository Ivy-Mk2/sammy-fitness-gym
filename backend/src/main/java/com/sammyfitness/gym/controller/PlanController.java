package com.sammyfitness.gym.controller;

import com.sammyfitness.gym.dto.PlanResponse;
import com.sammyfitness.gym.service.ClienteMembresiaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "6. Planes", description = "Planes de membresía disponibles")
@RestController
@RequestMapping("/api/planes")
@RequiredArgsConstructor
public class PlanController {

    private final ClienteMembresiaService service;

    @Operation(summary = "Listar planes", description = "Todos los autenticados. Ordenados por duración.")
    @GetMapping
    public List<PlanResponse> listar() {
        return service.listarPlanes();
    }
}

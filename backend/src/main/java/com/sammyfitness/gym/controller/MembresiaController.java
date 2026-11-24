package com.sammyfitness.gym.controller;

import com.sammyfitness.gym.dto.EstadoMembresiaRequest;
import com.sammyfitness.gym.dto.MembresiaResponse;
import com.sammyfitness.gym.security.UsuarioPrincipal;
import com.sammyfitness.gym.service.ClienteMembresiaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "5. Membresías", description = "Membresía del cliente y cambios de estado")
@RestController
@RequestMapping("/api/membresias")
@RequiredArgsConstructor
public class MembresiaController {

    private final ClienteMembresiaService service;

    @Operation(summary = "Mi membresía",
            description = "CLIENTE. Devuelve la membresía vigente o, si no hay, la más reciente, con los días restantes.")
    @ApiResponse(responseCode = "404", description = "El cliente no tiene membresías")
    @PreAuthorize("hasRole('CLIENTE')")
    @GetMapping("/mi-membresia")
    public MembresiaResponse miMembresia(@AuthenticationPrincipal UsuarioPrincipal cliente) {
        return service.miMembresia(cliente.id());
    }

    @Operation(summary = "Cambiar estado de una membresía",
            description = "ADMINISTRADOR o RECEPCIONISTA. ACTIVA, VENCIDA o CONGELADA. No se puede activar una "
                    + "membresía cuya fecha de vencimiento ya pasó.")
    @ApiResponse(responseCode = "404", description = "La membresía no existe")
    @ApiResponse(responseCode = "409", description = "No se puede activar (vencida o se cruza con otra activa)")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','RECEPCIONISTA')")
    @PatchMapping("/{id}/estado")
    public MembresiaResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody EstadoMembresiaRequest request) {
        return service.cambiarEstado(id, request.estado());
    }
}

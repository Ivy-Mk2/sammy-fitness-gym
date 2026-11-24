package com.sammyfitness.gym.controller;

import com.sammyfitness.gym.dto.*;
import com.sammyfitness.gym.model.Disciplina;
import com.sammyfitness.gym.security.UsuarioPrincipal;
import com.sammyfitness.gym.service.ClaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "2. Clases", description = "Catálogo, gestión de clases e inscritos")
@RestController
@RequestMapping("/api/clases")
@RequiredArgsConstructor
public class ClaseController {

    private final ClaseService claseService;

    @Operation(summary = "Listar clases (catálogo)",
            description = "Todos los autenticados. Filtros opcionales y combinables. Sin 'dia' devuelve las clases "
                    + "desde hoy. Incluye cupos ocupados y disponibles.")
    @GetMapping
    public List<ClaseResponse> listar(
            @Parameter(description = "Disciplina") @RequestParam(required = false) Disciplina disciplina,
            @Parameter(description = "Fecha exacta (yyyy-MM-dd)", example = "2026-12-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dia,
            @Parameter(description = "MANANA (<12:00), TARDE (12:00–17:59) o NOCHE (≥18:00)")
            @RequestParam(required = false) Horario horario,
            @Parameter(description = "Id del entrenador") @RequestParam(required = false) Long entrenadorId) {
        return claseService.listar(new ClaseFiltro(disciplina, dia, horario, entrenadorId));
    }

    @Operation(summary = "Detalle de una clase", description = "Todos los autenticados.")
    @ApiResponse(responseCode = "404", description = "La clase no existe")
    @GetMapping("/{id}")
    public ClaseResponse obtener(@PathVariable Long id) {
        return claseService.obtener(id);
    }

    @Operation(summary = "Crear clase",
            description = "ADMINISTRADOR o ENTRENADOR. El administrador debe indicar entrenadorId; el entrenador "
                    + "crea clases para sí mismo. Reglas: fecha no pasada, horaFin > horaInicio, sin cruces de "
                    + "horario del entrenador.")
    @ApiResponse(responseCode = "201", description = "Clase creada")
    @ApiResponse(responseCode = "400", description = "Datos inválidos (fecha pasada, horas, aforo 1-50…)")
    @ApiResponse(responseCode = "403", description = "Rol sin permiso o entrenador creando para otro")
    @ApiResponse(responseCode = "409", description = "El entrenador ya tiene una clase en ese horario")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ENTRENADOR')")
    @PostMapping
    public ResponseEntity<ClaseResponse> crear(@AuthenticationPrincipal UsuarioPrincipal actor,
                                               @Valid @RequestBody ClaseRequest request) {
        ClaseResponse creada = claseService.crear(actor, request);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creada.id()).toUri()).body(creada);
    }

    @Operation(summary = "Editar clase",
            description = "ADMINISTRADOR o el ENTRENADOR de la clase. No se edita una clase ya iniciada ni se reduce "
                    + "el aforo por debajo de las reservas confirmadas.")
    @ApiResponse(responseCode = "404", description = "La clase no existe")
    @ApiResponse(responseCode = "409", description = "Cruce de horario, aforo menor a reservas o modificación concurrente")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ENTRENADOR')")
    @PutMapping("/{id}")
    public ClaseResponse actualizar(@AuthenticationPrincipal UsuarioPrincipal actor, @PathVariable Long id,
                                    @Valid @RequestBody ClaseRequest request) {
        return claseService.actualizar(actor, id, request);
    }

    @Operation(summary = "Eliminar clase",
            description = "ADMINISTRADOR o el ENTRENADOR de la clase. Solo si no tiene reservas confirmadas.")
    @ApiResponse(responseCode = "204", description = "Clase eliminada")
    @ApiResponse(responseCode = "409", description = "La clase tiene reservas confirmadas")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ENTRENADOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@AuthenticationPrincipal UsuarioPrincipal actor, @PathVariable Long id) {
        claseService.eliminar(actor, id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Inscritos de una clase",
            description = "ADMINISTRADOR, RECEPCIONISTA o el ENTRENADOR de la clase. Reservas confirmadas y asistidas.")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ENTRENADOR','RECEPCIONISTA')")
    @GetMapping("/{id}/inscritos")
    public List<InscritoResponse> inscritos(@AuthenticationPrincipal UsuarioPrincipal actor, @PathVariable Long id) {
        return claseService.inscritos(actor, id);
    }
}

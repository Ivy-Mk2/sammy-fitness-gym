package com.sammyfitness.gym.controller;

import com.sammyfitness.gym.dto.AsistenciaRequest;
import com.sammyfitness.gym.dto.ReservaRequest;
import com.sammyfitness.gym.dto.ReservaResponse;
import com.sammyfitness.gym.security.UsuarioPrincipal;
import com.sammyfitness.gym.service.ReservaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "3. Reservas", description = "Reservas de clases de los clientes y asistencia")
@RestController
@RequestMapping("/api/reservas")
@RequiredArgsConstructor
public class ReservaController {

    private final ReservaService reservaService;

    @Operation(summary = "Reservar una clase",
            description = "CLIENTE. RN-01 membresía ACTIVA en la fecha de la clase · RN-02 aforo · "
                    + "RN-03 sin duplicados ni cruces de horario. Envía correo de confirmación en segundo plano.")
    @ApiResponse(responseCode = "201", description = "Reserva confirmada")
    @ApiResponse(responseCode = "404", description = "La clase no existe")
    @ApiResponse(responseCode = "409", description = "Aforo completo, reserva duplicada, cruce de horario, "
            + "membresía no activa, clase ya realizada o reserva simultánea")
    @PreAuthorize("hasRole('CLIENTE')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservaResponse reservar(@AuthenticationPrincipal UsuarioPrincipal cliente,
                                    @Valid @RequestBody ReservaRequest request) {
        return reservaService.reservar(cliente.id(), request.claseId());
    }

    @Operation(summary = "Mis reservas", description = "CLIENTE. Todas sus reservas, de la clase más reciente a la más antigua.")
    @PreAuthorize("hasRole('CLIENTE')")
    @GetMapping("/mis-reservas")
    public List<ReservaResponse> misReservas(@AuthenticationPrincipal UsuarioPrincipal cliente) {
        return reservaService.misReservas(cliente.id());
    }

    @Operation(summary = "Cancelar reserva",
            description = "CLIENTE, solo sus propias reservas (RN-04). Libera el cupo. No se cancelan clases ya realizadas.")
    @ApiResponse(responseCode = "200", description = "Reserva cancelada")
    @ApiResponse(responseCode = "403", description = "La reserva pertenece a otro cliente")
    @ApiResponse(responseCode = "404", description = "La reserva no existe")
    @ApiResponse(responseCode = "409", description = "La reserva no está confirmada o la clase ya se realizó")
    @PreAuthorize("hasRole('CLIENTE')")
    @DeleteMapping("/{id}")
    public ReservaResponse cancelar(@AuthenticationPrincipal UsuarioPrincipal cliente, @PathVariable Long id) {
        return reservaService.cancelar(cliente.id(), id);
    }

    @Operation(summary = "Registrar asistencia",
            description = "ADMINISTRADOR o el ENTRENADOR de la clase. asistio=true → ASISTIDA, false → CONFIRMADA. "
                    + "Solo desde el día de la clase.")
    @ApiResponse(responseCode = "409", description = "Reserva cancelada o clase aún no realizada")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ENTRENADOR')")
    @PatchMapping("/{id}/asistencia")
    public ReservaResponse asistencia(@AuthenticationPrincipal UsuarioPrincipal actor, @PathVariable Long id,
                                      @Valid @RequestBody AsistenciaRequest request) {
        return reservaService.marcarAsistencia(actor, id, request.asistio());
    }
}

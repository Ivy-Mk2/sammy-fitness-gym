package com.sammyfitness.gym.controller;

import com.sammyfitness.gym.dto.MembresiaRequest;
import com.sammyfitness.gym.dto.MembresiaResponse;
import com.sammyfitness.gym.dto.SocioRequest;
import com.sammyfitness.gym.dto.SocioResponse;
import com.sammyfitness.gym.service.ClienteMembresiaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@Tag(name = "4. Socios", description = "Gestión de socios (clientes) y asignación de membresías")
@RestController
@RequestMapping("/api/socios")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMINISTRADOR','RECEPCIONISTA')")
public class SocioController {

    private final ClienteMembresiaService service;

    @Operation(summary = "Listar socios",
            description = "ADMINISTRADOR o RECEPCIONISTA. Búsqueda opcional por nombre, apellido, DNI o correo. "
                    + "Incluye la membresía vigente (o la más reciente).")
    @GetMapping
    public List<SocioResponse> listar(@Parameter(description = "Texto a buscar") @RequestParam(required = false) String q) {
        return service.listarSocios(q);
    }

    @Operation(summary = "Detalle de un socio", description = "ADMINISTRADOR o RECEPCIONISTA.")
    @ApiResponse(responseCode = "404", description = "El socio no existe")
    @GetMapping("/{id}")
    public SocioResponse obtener(@PathVariable Long id) {
        return service.obtenerSocio(id);
    }

    @Operation(summary = "Registrar socio",
            description = "ADMINISTRADOR o RECEPCIONISTA. Crea un usuario con rol CLIENTE. Contraseña obligatoria (mín. 8).")
    @ApiResponse(responseCode = "201", description = "Socio registrado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos (DNI 8 dígitos, teléfono 9 dígitos, correo…)")
    @ApiResponse(responseCode = "409", description = "DNI o correo ya registrados")
    @PostMapping
    public ResponseEntity<SocioResponse> crear(@Valid @RequestBody SocioRequest request) {
        SocioResponse creado = service.crearSocio(request);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creado.id()).toUri()).body(creado);
    }

    @Operation(summary = "Editar socio",
            description = "ADMINISTRADOR o RECEPCIONISTA. password vacía = no se cambia; activo=false desactiva el acceso.")
    @ApiResponse(responseCode = "404", description = "El socio no existe")
    @ApiResponse(responseCode = "409", description = "DNI o correo ya registrados por otro usuario")
    @PutMapping("/{id}")
    public SocioResponse actualizar(@PathVariable Long id, @Valid @RequestBody SocioRequest request) {
        return service.actualizarSocio(id, request);
    }

    @Operation(summary = "Asignar membresía",
            description = "ADMINISTRADOR o RECEPCIONISTA. RN-05: fechaVencimiento = fechaInicio + duración del plan. "
                    + "Sin fechaInicio empieza hoy.")
    @ApiResponse(responseCode = "201", description = "Membresía asignada")
    @ApiResponse(responseCode = "404", description = "El socio o el plan no existen")
    @ApiResponse(responseCode = "409", description = "Ya tiene una membresía activa en ese periodo")
    @PostMapping("/{id}/membresias")
    public ResponseEntity<MembresiaResponse> asignarMembresia(@PathVariable Long id,
                                                              @Valid @RequestBody MembresiaRequest request) {
        MembresiaResponse m = service.asignarMembresia(id, request);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/membresias/{id}").buildAndExpand(m.id()).toUri()).body(m);
    }
}

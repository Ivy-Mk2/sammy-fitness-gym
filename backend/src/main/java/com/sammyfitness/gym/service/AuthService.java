package com.sammyfitness.gym.service;

import com.sammyfitness.gym.dto.LoginRequest;
import com.sammyfitness.gym.dto.LoginResponse;
import com.sammyfitness.gym.security.JwtService;
import com.sammyfitness.gym.security.UsuarioPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    /**
     * Verifica correo y contraseña (BCrypt) y emite un JWT.
     * Si falla, AuthenticationManager lanza AuthenticationException → 401 "Credenciales incorrectas".
     */
    public LoginResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.correo().trim(), request.password()));

        UsuarioPrincipal usuario = (UsuarioPrincipal) auth.getPrincipal();
        return new LoginResponse(jwtService.generarToken(usuario), usuario.rol(), usuario.nombre());
    }
}

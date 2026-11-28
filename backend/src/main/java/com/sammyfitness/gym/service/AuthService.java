package com.sammyfitness.gym.service;

import com.sammyfitness.gym.dto.CambiarPasswordRequest;
import com.sammyfitness.gym.dto.LoginRequest;
import com.sammyfitness.gym.dto.LoginResponse;
import com.sammyfitness.gym.exception.DatosInvalidosException;
import com.sammyfitness.gym.exception.RecursoNoEncontradoException;
import com.sammyfitness.gym.model.Usuario;
import com.sammyfitness.gym.repository.UsuarioRepository;
import com.sammyfitness.gym.security.JwtService;
import com.sammyfitness.gym.security.UsuarioPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

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

    @Transactional
    public void cambiarPassword(Long usuarioId, CambiarPasswordRequest request) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        if (!passwordEncoder.matches(request.passwordActual(), usuario.getPassword())) {
            throw new DatosInvalidosException("La contraseña actual es incorrecta");
        }
        if (passwordEncoder.matches(request.passwordNueva(), usuario.getPassword())) {
            throw new DatosInvalidosException("La nueva contraseña debe ser diferente a la actual");
        }

        usuario.setPassword(passwordEncoder.encode(request.passwordNueva()));
        usuarioRepository.save(usuario);
    }
}
package com.sammyfitness.gym.security;

import com.sammyfitness.gym.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Carga usuarios por correo (el correo es el "username" del login). */
@Service
@RequiredArgsConstructor
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        return usuarioRepository.findByCorreoIgnoreCase(correo)
                .map(UsuarioPrincipal::desde)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
    }
}

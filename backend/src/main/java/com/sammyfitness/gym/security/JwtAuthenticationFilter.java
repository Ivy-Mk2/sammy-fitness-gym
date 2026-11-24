package com.sammyfitness.gym.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Lee "Authorization: Bearer <token>", lo valida y autentica la petición.
 * Si el token falta o es inválido no autentica: Spring Security responde 401 en las rutas protegidas.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIJO = "Bearer ";

    private final JwtService jwtService;
    private final UsuarioDetailsService usuarioDetailsService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith(PREFIJO)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = header.substring(PREFIJO.length()).trim();
            jwtService.extraerCorreoSiValido(token).ifPresent(correo -> autenticar(correo, request));
        }
        chain.doFilter(request, response);
    }

    private void autenticar(String correo, HttpServletRequest request) {
        try {
            UsuarioPrincipal usuario = (UsuarioPrincipal) usuarioDetailsService.loadUserByUsername(correo);
            if (!usuario.isEnabled()) {
                return; // usuario desactivado después de emitir el token
            }
            var auth = new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (UsernameNotFoundException e) {
            // usuario eliminado: la petición sigue sin autenticar
        }
    }
}

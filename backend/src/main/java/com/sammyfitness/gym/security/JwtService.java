package com.sammyfitness.gym.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

/** Genera y valida tokens JWT firmados con HMAC-SHA. */
@Service
public class JwtService {

    private static final int LONGITUD_MINIMA_CLAVE = 32;

    private final SecretKey clave;
    private final long expiracionMs;

    public JwtService(@Value("${app.jwt.secret}") String secreto,
                      @Value("${app.jwt.expiration-ms}") long expiracionMs) {
        if (secreto == null || secreto.length() < LONGITUD_MINIMA_CLAVE) {
            throw new IllegalStateException(
                    "JWT_SECRET debe tener al menos " + LONGITUD_MINIMA_CLAVE + " caracteres");
        }
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.expiracionMs = expiracionMs;
    }

    public String generarToken(UsuarioPrincipal usuario) {
        Date ahora = new Date();
        return Jwts.builder()
                .subject(usuario.correo())
                .claim("id", usuario.id())
                .claim("rol", usuario.rol().name())
                .claim("nombre", usuario.nombre())
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + expiracionMs))
                .signWith(clave)
                .compact();
    }

    /** Devuelve el correo (subject) si el token es válido y no ha expirado. */
    public Optional<String> extraerCorreoSiValido(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(clave).build()
                    .parseSignedClaims(token).getPayload();
            return Optional.ofNullable(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public long getExpiracionMs() {
        return expiracionMs;
    }
}

package com.sammyfitness.gym.security;

import com.sammyfitness.gym.model.Rol;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRETO = "clave-de-pruebas-unitarias-no-usar-en-produccion-0123456789";
    private final UsuarioPrincipal usuario =
            new UsuarioPrincipal(1L, "ana@correo.test", "hash", "Ana Soto", Rol.CLIENTE, true);

    @Test
    void generaYValidaToken() {
        JwtService jwt = new JwtService(SECRETO, 60_000);

        String token = jwt.generarToken(usuario);

        assertThat(jwt.extraerCorreoSiValido(token)).contains("ana@correo.test");
    }

    @Test
    void tokenExpiradoNoEsValido() {
        JwtService jwt = new JwtService(SECRETO, -1_000);

        assertThat(jwt.extraerCorreoSiValido(jwt.generarToken(usuario))).isEmpty();
    }

    @Test
    void tokenFirmadoConOtraClaveNoEsValido() {
        String token = new JwtService(SECRETO, 60_000).generarToken(usuario);
        JwtService otro = new JwtService("otra-clave-distinta-de-al-menos-32-caracteres!!", 60_000);

        assertThat(otro.extraerCorreoSiValido(token)).isEmpty();
    }

    @Test
    void rechazaClaveCorta() {
        assertThatThrownBy(() -> new JwtService("corta", 60_000))
                .isInstanceOf(IllegalStateException.class);
    }
}

package com.sammyfitness.gym.security;

import com.sammyfitness.gym.model.Rol;
import com.sammyfitness.gym.model.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Usuario autenticado. Se obtiene en los controladores con @AuthenticationPrincipal.
 * La autoridad es "ROLE_<ROL>" para poder usar hasRole('ADMINISTRADOR') en @PreAuthorize.
 */
public record UsuarioPrincipal(Long id, String correo, String password, String nombre,
                               Rol rol, boolean activo) implements UserDetails {

    public static UsuarioPrincipal desde(Usuario u) {
        return new UsuarioPrincipal(u.getId(), u.getCorreo(), u.getPassword(),
                u.getNombreCompleto(), u.getRol(), u.isActivo());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return correo;
    }

    @Override
    public boolean isEnabled() {
        return activo;
    }
}

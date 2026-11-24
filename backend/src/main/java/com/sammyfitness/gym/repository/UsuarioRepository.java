package com.sammyfitness.gym.repository;

import com.sammyfitness.gym.model.Rol;
import com.sammyfitness.gym.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreoIgnoreCase(String correo);

    boolean existsByDni(String dni);

    boolean existsByCorreoIgnoreCase(String correo);

    List<Usuario> findByRol(Rol rol);

    long countByRol(Rol rol);
}

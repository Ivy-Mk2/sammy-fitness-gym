package com.sammyfitness.gym.repository;

import com.sammyfitness.gym.model.Rol;
import com.sammyfitness.gym.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreoIgnoreCase(String correo);

    boolean existsByDni(String dni);

    boolean existsByCorreoIgnoreCase(String correo);

    boolean existsByDniAndIdNot(String dni, Long id);

    boolean existsByCorreoIgnoreCaseAndIdNot(String correo, Long id);

    List<Usuario> findByRol(Rol rol);

    long countByRol(Rol rol);

    Optional<Usuario> findByIdAndRol(Long id, Rol rol);

    /** Busca por rol y, opcionalmente, por texto en nombres, apellidos, DNI o correo. */
    @Query("""
            select u from Usuario u
            where u.rol = :rol
              and (:texto is null
                   or lower(u.nombres) like lower(concat('%', :texto, '%'))
                   or lower(u.apellidos) like lower(concat('%', :texto, '%'))
                   or u.dni like concat('%', :texto, '%')
                   or lower(u.correo) like lower(concat('%', :texto, '%')))
            order by u.apellidos, u.nombres
            """)
    List<Usuario> buscarPorRol(@Param("rol") Rol rol, @Param("texto") String texto);
}

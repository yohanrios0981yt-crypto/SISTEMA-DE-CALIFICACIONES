package edu.universidad.calificaciones.service;

import edu.universidad.calificaciones.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarios;

    public UsuarioDetailsService(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public UserDetails loadUserByUsername(String correo)
            throws UsernameNotFoundException {

        var usuario = usuarios.findByCorreo(correo)
            .orElseThrow(() -> new UsernameNotFoundException(
                "No existe una cuenta con ese correo"
            ));

        return User.withUsername(usuario.getCorreo())
            .password(usuario.getPassword())
            .roles(usuario.getRol().name())
            .build();
    }
}
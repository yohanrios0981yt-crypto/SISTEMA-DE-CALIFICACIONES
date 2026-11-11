package edu.universidad.calificaciones.web;

import edu.universidad.calificaciones.model.Rol;
import edu.universidad.calificaciones.model.Usuario;
import edu.universidad.calificaciones.repository.UsuarioRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Locale;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;

    public AuthController(
            UsuarioRepository usuarios,
            PasswordEncoder passwordEncoder) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
    }

    public record Registro(
        @NotBlank String nombre,
        @Email @NotBlank String correo,
        @NotBlank @Size(min = 8) String password
    ) {}

    public record UsuarioRespuesta(
        Long id,
        String nombre,
        String correo,
        String rol
    ) {}

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioRespuesta registrar(@Valid @RequestBody Registro datos) {
        String correo = datos.correo().toLowerCase(Locale.ROOT);

        if (usuarios.findByCorreo(correo).isPresent()) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Ese correo ya está registrado"
            );
        }

        Usuario nuevo = new Usuario(
            datos.nombre(),
            correo,
            passwordEncoder.encode(datos.password()),
            Rol.ESTUDIANTE
        );

        Usuario guardado = usuarios.save(nuevo);

        return new UsuarioRespuesta(
            guardado.getId(),
            guardado.getNombre(),
            guardado.getCorreo(),
            guardado.getRol().name()
        );
    }

    @GetMapping("/perfil")
    @SecurityRequirement(name = "basicAuth")
    public UsuarioRespuesta perfil(Authentication auth) {
        Usuario usuario = usuarios.findByCorreo(auth.getName())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Usuario no encontrado"
            ));

        return new UsuarioRespuesta(
            usuario.getId(),
            usuario.getNombre(),
            usuario.getCorreo(),
            usuario.getRol().name()
        );
    }
}

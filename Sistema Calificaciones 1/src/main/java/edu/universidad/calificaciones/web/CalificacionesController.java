package edu.universidad.calificaciones.web;

import edu.universidad.calificaciones.model.Calificacion;
import edu.universidad.calificaciones.model.Asignatura;
import edu.universidad.calificaciones.model.Matricula;
import edu.universidad.calificaciones.model.Rol;
import edu.universidad.calificaciones.model.Usuario;
import edu.universidad.calificaciones.repository.AsignaturaRepository;
import edu.universidad.calificaciones.repository.CalificacionRepository;
import edu.universidad.calificaciones.repository.MatriculaRepository;
import edu.universidad.calificaciones.repository.UsuarioRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
@SecurityRequirement(name = "basicAuth")
@Transactional(readOnly = true)
public class CalificacionesController {

    private final CalificacionRepository calificaciones;
    private final MatriculaRepository matriculas;
    private final AsignaturaRepository asignaturas;
    private final UsuarioRepository usuarios;

    public CalificacionesController(
            CalificacionRepository calificaciones,
            MatriculaRepository matriculas,
            AsignaturaRepository asignaturas,
            UsuarioRepository usuarios) {
        this.calificaciones = calificaciones;
        this.matriculas = matriculas;
        this.asignaturas = asignaturas;
        this.usuarios = usuarios;
    }

    public record NotaRequest(
        @NotBlank @Size(max = 80) String evaluacion,
        @NotNull @DecimalMin("0.00") @DecimalMax("5.00") BigDecimal nota
    ) {}

    public record NotaRespuesta(
        Long id,
        String estudiante,
        String asignatura,
        String evaluacion,
        BigDecimal nota,
        LocalDateTime fecha
    ) {}

    public record MatriculaRespuesta(
        Long id,
        String estudiante,
        String correo,
        String asignatura,
        String codigo
    ) {}

    public record AsignaturaRespuesta(Long id, String nombre, String codigo) {}

    public record MatriculaRequest(
        @Email @NotBlank String correoEstudiante,
        @NotNull Long asignaturaId
    ) {}

    private NotaRespuesta convertir(Calificacion c) {
        return new NotaRespuesta(
            c.getId(),
            c.getMatricula().getEstudiante().getNombre(),
            c.getMatricula().getAsignatura().getNombre(),
            c.getEvaluacion(),
            c.getNota(),
            c.getFecha()
        );
    }

    @GetMapping("/estudiante/mis-notas")
    @Operation(summary = "Consultar las notas del estudiante autenticado")
    public List<NotaRespuesta> misNotas(Authentication auth) {
        return calificaciones
            .findByMatriculaEstudianteCorreo(auth.getName())
            .stream()
            .map(this::convertir)
            .toList();
    }

    @GetMapping("/profesor/matriculas")
    @Operation(summary = "Ver estudiantes matriculados en las asignaturas del profesor")
    public List<MatriculaRespuesta> misMatriculas(Authentication auth) {
        return matriculas
            .findByAsignaturaProfesorCorreo(auth.getName())
            .stream()
            .map(m -> new MatriculaRespuesta(
                m.getId(),
                m.getEstudiante().getNombre(),
                m.getEstudiante().getCorreo(),
                m.getAsignatura().getNombre(),
                m.getAsignatura().getCodigo()
            ))
            .toList();
    }

    @GetMapping("/profesor/asignaturas")
    @Operation(summary = "Listar las asignaturas del profesor autenticado")
    public List<AsignaturaRespuesta> misAsignaturas(Authentication auth) {
        return asignaturas.findByProfesorCorreo(auth.getName())
            .stream()
            .map(a -> new AsignaturaRespuesta(a.getId(), a.getNombre(), a.getCodigo()))
            .toList();
    }

    @PostMapping("/profesor/matriculas")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    @Operation(summary = "Matricular un estudiante registrado en una asignatura propia")
    public MatriculaRespuesta matricularEstudiante(
            @Valid @RequestBody MatriculaRequest datos,
            Authentication auth) {
        String correo = datos.correoEstudiante().trim().toLowerCase(Locale.ROOT);
        Usuario estudiante = usuarios.findByCorreo(correo)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "No existe un estudiante con ese correo"
            ));

        if (estudiante.getRol() != Rol.ESTUDIANTE) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "El correo no pertenece a una cuenta de estudiante"
            );
        }

        Asignatura asignatura = asignaturas
            .findByIdAndProfesorCorreo(datos.asignaturaId(), auth.getName())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Asignatura no encontrada entre tus cursos"
            ));

        if (matriculas.existsByEstudianteIdAndAsignaturaId(
                estudiante.getId(), asignatura.getId())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "Ese estudiante ya está matriculado en esa asignatura"
            );
        }

        Matricula nueva = matriculas.save(new Matricula(estudiante, asignatura));

        return new MatriculaRespuesta(
            nueva.getId(),
            estudiante.getNombre(),
            estudiante.getCorreo(),
            asignatura.getNombre(),
            asignatura.getCodigo()
        );
    }

    @GetMapping("/profesor/matriculas/{matriculaId}/notas")
    @Operation(summary = "Consultar notas de una matrícula propia")
    public List<NotaRespuesta> notasDeMatricula(
            @PathVariable Long matriculaId,
            Authentication auth) {
        var matricula = matriculas
            .findByIdAndAsignaturaProfesorCorreo(matriculaId, auth.getName())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Matrícula no encontrada"
            ));

        return calificaciones
            .findByMatriculaAsignaturaProfesorCorreo(auth.getName())
            .stream()
            .filter(c -> c.getMatricula().getId().equals(matricula.getId()))
            .map(this::convertir)
            .toList();
    }

    @PostMapping("/profesor/matriculas/{matriculaId}/notas")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    @Operation(summary = "Registrar una nota para un estudiante matriculado")
    public NotaRespuesta crearNota(
            @PathVariable Long matriculaId,
            @Valid @RequestBody NotaRequest datos,
            Authentication auth) {
        var matricula = matriculas
            .findByIdAndAsignaturaProfesorCorreo(matriculaId, auth.getName())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Matrícula no encontrada"
            ));

        var nota = calificaciones.save(
            new Calificacion(matricula, datos.evaluacion(), datos.nota())
        );

        return convertir(nota);
    }

    @PutMapping("/profesor/notas/{id}")
    @Transactional
    @Operation(summary = "Actualizar una nota del profesor autenticado")
    public NotaRespuesta actualizarNota(
            @PathVariable Long id,
            @Valid @RequestBody NotaRequest datos,
            Authentication auth) {
        var nota = calificaciones.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Nota no encontrada"
            ));

        String correoProfesor = nota.getMatricula()
            .getAsignatura()
            .getProfesor()
            .getCorreo();

        if (!correoProfesor.equals(auth.getName())) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Nota no encontrada"
            );
        }

        nota.actualizar(datos.evaluacion(), datos.nota());
        return convertir(calificaciones.save(nota));
    }
}

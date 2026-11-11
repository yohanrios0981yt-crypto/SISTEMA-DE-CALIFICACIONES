package edu.universidad.calificaciones.repository;

import edu.universidad.calificaciones.model.Matricula;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatriculaRepository extends JpaRepository<Matricula, Long> {
    List<Matricula> findByEstudianteCorreo(String correo);

    List<Matricula> findByAsignaturaProfesorCorreo(String correo);

    Optional<Matricula> findByIdAndAsignaturaProfesorCorreo(
        Long id,
        String correo
    );

    boolean existsByEstudianteIdAndAsignaturaId(Long estudianteId, Long asignaturaId);
}

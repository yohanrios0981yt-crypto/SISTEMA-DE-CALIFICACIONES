package edu.universidad.calificaciones.repository;

import edu.universidad.calificaciones.model.Calificacion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CalificacionRepository
        extends JpaRepository<Calificacion, Long> {

    List<Calificacion> findByMatriculaEstudianteCorreo(String correo);

    List<Calificacion> findByMatriculaAsignaturaProfesorCorreo(String correo);
}
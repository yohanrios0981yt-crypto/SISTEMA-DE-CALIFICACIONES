package edu.universidad.calificaciones.repository;

import edu.universidad.calificaciones.model.Asignatura;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AsignaturaRepository extends JpaRepository<Asignatura, Long> {
    List<Asignatura> findByProfesorCorreo(String correo);

    Optional<Asignatura> findByIdAndProfesorCorreo(Long id, String correo);
}

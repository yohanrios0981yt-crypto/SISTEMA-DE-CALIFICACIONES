package edu.universidad.calificaciones.model;

import jakarta.persistence.*;

@Entity
@Table(name = "MATRICULA")
public class Matricula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ESTUDIANTE_ID", nullable = false)
    private Usuario estudiante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ASIGNATURA_ID", nullable = false)
    private Asignatura asignatura;

    protected Matricula() {
    }

    public Matricula(Usuario estudiante, Asignatura asignatura) {
        this.estudiante = estudiante;
        this.asignatura = asignatura;
    }

    public Long getId() {
        return id;
    }

    public Usuario getEstudiante() {
        return estudiante;
    }

    public Asignatura getAsignatura() {
        return asignatura;
    }
}
package edu.universidad.calificaciones.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "CALIFICACION")
public class Calificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "MATRICULA_ID", nullable = false)
    private Matricula matricula;

    @Column(name = "EVALUACION", nullable = false, length = 80)
    private String evaluacion;

    @Column(name = "NOTA", nullable = false, precision = 4, scale = 2)
    private BigDecimal nota;

    @Column(name = "FECHA", nullable = false)
    private LocalDateTime fecha;

    protected Calificacion() {
    }

    public Calificacion(Matricula matricula, String evaluacion, BigDecimal nota) {
        this.matricula = matricula;
        this.evaluacion = evaluacion;
        this.nota = nota;
        this.fecha = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Matricula getMatricula() {
        return matricula;
    }

    public String getEvaluacion() {
        return evaluacion;
    }

    public BigDecimal getNota() {
        return nota;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void actualizar(String evaluacion, BigDecimal nota) {
        this.evaluacion = evaluacion;
        this.nota = nota;
        this.fecha = LocalDateTime.now();
    }
}
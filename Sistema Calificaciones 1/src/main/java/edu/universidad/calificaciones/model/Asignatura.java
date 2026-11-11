package edu.universidad.calificaciones.model;

import jakarta.persistence.*;

@Entity
@Table(name = "ASIGNATURA")
public class Asignatura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "NOMBRE", nullable = false, length = 120)
    private String nombre;

    @Column(name = "CODIGO", nullable = false, unique = true, length = 20)
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "PROFESOR_ID", nullable = false)
    private Usuario profesor;

    protected Asignatura() {
    }

    public Asignatura(String nombre, String codigo, Usuario profesor) {
        this.nombre = nombre;
        this.codigo = codigo;
        this.profesor = profesor;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getCodigo() {
        return codigo;
    }

    public Usuario getProfesor() {
        return profesor;
    }
}
package com.mycompany.vcsystems.modelo.entidades;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuario")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long idUsuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false)
    private Rol rol;

    @NotBlank
    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Email
    @NotBlank
    @Column(name = "correo", nullable = false, unique = true)
    private String correo;

    @NotBlank
    @Column(name = "contrasena", nullable = false)
    private String contrasena;

    @CreatedDate
    @Column(name = "creado_at", nullable = false, updatable = false)
    private LocalDateTime creadoAt;

    @LastModifiedDate
    @Column(name = "actualizado_at", nullable = false)
    private LocalDateTime actualizadoAt;

    public enum Rol {
        ADMIN,
        GERENTE,
        TECNICO,
        CLIENTE
    }
}

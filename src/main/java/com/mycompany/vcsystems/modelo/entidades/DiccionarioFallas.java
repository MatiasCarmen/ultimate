package com.mycompany.vcsystems.modelo.entidades;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "diccionario_fallas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
/**
 *
 * @author MatiasCarmen
 */
public class DiccionarioFallas {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_falla")
    private Long idFalla;

    @NotBlank(message = "El código de falla es obligatorio")
    @Column(name = "codigo_falla", unique = true, nullable = false)
    private String codigoFalla;

    @Column(name = "descripcion")
    private String descripcion;

    @CreatedDate
    @Column(name = "creado_at", nullable = false, updatable = false)
    private LocalDateTime creadoAt;

    @LastModifiedDate
    @Column(name = "actualizado_at", nullable = false)
    private LocalDateTime actualizadoAt;
}

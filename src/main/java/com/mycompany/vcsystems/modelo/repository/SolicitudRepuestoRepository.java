package com.mycompany.vcsystems.modelo.repository;

import com.mycompany.vcsystems.modelo.entidades.SolicitudRepuesto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
/**
 *
 * @author MatiasCarmen
 */
public interface SolicitudRepuestoRepository extends JpaRepository<SolicitudRepuesto, Long> {

    @Query("SELECT s FROM SolicitudRepuesto s WHERE s.estado = :estado")
    List<SolicitudRepuesto> findByEstado(@Param("estado") String estado);
}

package com.mycompany.vcsystems.modelo.repository;

import com.mycompany.vcsystems.modelo.entidades.DiccionarioFallas;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
/**
 *
 * @author MatiasCarmen
 */
public interface DiccionarioFallasRepository extends JpaRepository<DiccionarioFallas, Long> {
}


package com.mycompany.vcsystems.modelo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.mycompany.vcsystems.modelo.entidades.Cliente;

@Repository
/**
 *
 * @author MatiasCarmen
 */
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
}

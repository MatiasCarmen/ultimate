package com.mycompany.vcsystems.api.controlador;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import com.mycompany.vcsystems.modelo.repository.SolicitudRepuestoRepository;
import com.mycompany.vcsystems.modelo.entidades.SolicitudRepuesto;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/solicitudes")


/**
 *
 * @author MatiasCarmen
 */

public class SolicitudControlador {

    @Autowired
    private SolicitudRepuestoRepository solicitudRepository;

    @PostMapping
    public ResponseEntity<SolicitudRepuesto> crear(@Valid @RequestBody SolicitudRepuesto solicitud) {
        SolicitudRepuesto nuevaSolicitud = solicitudRepository.save(solicitud);
        return ResponseEntity.status(201).body(nuevaSolicitud);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TECNICO', 'GERENTE')")
    public ResponseEntity<SolicitudRepuesto> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody SolicitudRepuesto solicitud) {
        return solicitudRepository.findById(id)
            .map(existing -> {
                solicitud.setIdSolicitud(id);
                return ResponseEntity.ok(solicitudRepository.save(solicitud));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<?> listarPorEstado(@RequestParam(required = false) String estado) {
        if (estado != null) {
            return ResponseEntity.ok(solicitudRepository.findByEstado(estado));
        }
        return ResponseEntity.ok(solicitudRepository.findAll());
    }
}

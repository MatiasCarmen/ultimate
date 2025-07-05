package com.mycompany.vcsystems.api.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import com.mycompany.vcsystems.modelo.service.IncidenciaService;
import com.mycompany.vcsystems.modelo.entidades.Incidencia;
import lombok.Data;
import com.mycompany.vcsystems.modelo.entidades.Usuario;
import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/incidencias")
public class IncidenciaController {

    @Autowired
    private IncidenciaService incidenciaService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public List<Incidencia> listarTodas() {
        return incidenciaService.listAll();
    }

    @PostMapping
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<Incidencia> crear(@Valid @RequestBody Incidencia incidencia) {
        Incidencia nuevaIncidencia = incidenciaService.crearIncidencia(incidencia);
        return ResponseEntity.status(201).body(nuevaIncidencia);
    }

    @PutMapping("/{id}/tecnico")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<?> asignarTecnico(
            @PathVariable Long id,
            @RequestBody AsignacionTecnicoRequest request) {
        return incidenciaService.asignarTecnico(id, request.getTecnico())
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('TECNICO', 'GERENTE')")
    public ResponseEntity<?> cambiarEstado(
            @PathVariable Long id,
            @RequestBody CambioEstadoRequest request) {
        return incidenciaService.cambiarEstado(id, request.getEstado())
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    // Manejador de excepciones para ValidationException
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, String>> handleValidationException(ValidationException ex) {
        return ResponseEntity.badRequest()
            .body(Map.of("error", ex.getMessage()));
    }

    @Data
    static class AsignacionTecnicoRequest {
        private Usuario tecnico;
    }

    @Data
    static class CambioEstadoRequest {
        private Incidencia.Estado estado;
    }
}

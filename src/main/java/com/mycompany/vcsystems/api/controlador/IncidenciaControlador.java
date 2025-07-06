package com.mycompany.vcsystems.api.controlador;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import com.mycompany.vcsystems.modelo.service.IncidenciaService;
import com.mycompany.vcsystems.modelo.service.IncidenciaOperationResult;
import com.mycompany.vcsystems.modelo.entidades.Incidencia;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import com.mycompany.vcsystems.modelo.entidades.Usuario;
import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/incidencias")
@Slf4j // Agregar para logging


/**
 *
 * @author MatiasCarmen
 */

public class IncidenciaControlador {

    @Autowired
    private IncidenciaService incidenciaService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public List<Incidencia> listarTodas() {
        return incidenciaService.listAll();
    }

    @PostMapping
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<?> crear(@Valid @RequestBody Incidencia incidencia) {
        try {
            Incidencia nuevaIncidencia = incidenciaService.crearIncidencia(incidencia);
            return ResponseEntity.status(HttpStatus.CREATED).body(nuevaIncidencia);
        } catch (ValidationException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage(), "success", false));
        } catch (Exception e) {
            log.error("Error al crear incidencia", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error interno del servidor", "success", false));
        }
    }

    @PutMapping("/{id}/tecnico")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<?> asignarTecnico(
            @PathVariable Long id,
            @RequestBody AsignacionTecnicoRequest request) {

        IncidenciaOperationResult result = incidenciaService.asignarTecnico(id, request.getTecnico());

        return createResponseFromResult(result);
    }

    @PutMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('TECNICO', 'GERENTE')")
    public ResponseEntity<?> cambiarEstado(
            @PathVariable Long id,
            @RequestBody CambioEstadoRequest request) {

        IncidenciaOperationResult result = incidenciaService.cambiarEstado(id, request.getEstado());

        return createResponseFromResult(result);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'TECNICO', 'CLIENTE')")
    public ResponseEntity<?> buscarPorId(@PathVariable Long id) {
        IncidenciaOperationResult result = incidenciaService.buscarPorId(id);
        return createResponseFromResult(result);
    }

    @GetMapping("/estadisticas")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<?> obtenerEstadisticas() {
        try {
            IncidenciaService.IncidenciaStatistics stats = incidenciaService.getStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error obteniendo estadísticas de incidencias", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error obteniendo estadísticas", "success", false));
        }
    }

    private ResponseEntity<?> createResponseFromResult(IncidenciaOperationResult result) {
        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getIncidencia());
        }

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("timestamp", java.time.LocalDateTime.now().toString());

        if (result.isNotFound()) {
            errorResponse.put("error", "Recurso no encontrado");
            errorResponse.put("message", result.getMessage());
            errorResponse.put("code", result.getErrorCode());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } else if (result.isBusinessError()) {
            errorResponse.put("error", "Error de validación de negocio");
            errorResponse.put("message", result.getMessage());
            errorResponse.put("code", result.getErrorCode());
            return ResponseEntity.badRequest().body(errorResponse);
        } else {
            errorResponse.put("error", "Error interno del servidor");
            errorResponse.put("message", result.getMessage());
            errorResponse.put("code", result.getErrorCode());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
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

package com.mycompany.vcsystems.api.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import com.mycompany.vcsystems.modelo.service.UsuarioService;
import com.mycompany.vcsystems.modelo.entidades.Usuario; // Asegúrate de importar la clase Usuario
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:8081") // Permitir CORS para el frontend
@Slf4j // Agregar para logging
public class AuthController {

    private final UsuarioService usuarioService;

    // Inyección por constructor para evitar dependencias circulares
    public AuthController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
                    Map<String, Object> response = new HashMap<>();
                    response.put("token", token);
                    response.put("success", true);

                    // Agregar información del usuario que espera el frontend
                    Map<String, Object> usuario = new HashMap<>();
                    usuario.put("idUsuario", user.getIdUsuario());
                    usuario.put("nombre", user.getNombre());
                    usuario.put("correo", user.getCorreo());
                    usuario.put("rol", user.getRol().toString());
                    response.put("usuario", usuario);

                    // Corregir rutas de redirección para que coincidan con el frontend
                    response.put("redirect", switch (user.getRol()) {
                        case GERENTE -> "/pages/gerente.html";
                        case TECNICO -> "/pages/tecnico.html";
                        case CLIENTE -> "/pages/cliente.html";
                        default -> "/pages/login.html";
                    });

                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Credenciales inválidas", "success", false)));
        } catch (Exception e) {
            // Registrar el stack trace completo para depuración y monitoreo
            log.error("Error interno durante el proceso de login para usuario: {}",
                request.getCorreo() != null ? maskSensitiveData(request.getCorreo()) : "unknown", e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error interno del servidor", "success", false));
        }
    }

    // Endpoint para verificar el estado del servidor
    @GetMapping("/status")
    public ResponseEntity<?> status() {
        return ResponseEntity.ok(Map.of("status", "OK", "service", "VCSystems Auth"));
    }

    @Data
    static class LoginRequest {
        @Email(message = "Formato de email inválido")
        @NotBlank(message = "El correo es requerido")
        private String correo;

        @NotBlank(message = "La contraseña es requerida")
        private String contrasena;
    }

    @Data
    static class RefreshTokenRequest {
        @NotBlank(message = "El token es requerido")
        private String oldToken;
    }

    @Data
    static class LogoutRequest {
        @NotBlank(message = "El token es requerido")
        private String token;

        private String refreshToken;
    }

    /**
     * Enmascara datos sensibles para logs seguros (reutilizar del NotificacionService)
     */
    private String maskSensitiveData(String data) {
        if (data == null || data.isEmpty()) {
            return "***";
        }

        if (data.contains("@")) {
            String[] parts = data.split("@");
            if (parts.length == 2) {
                String localPart = parts[0].length() > 2 ?
                    parts[0].charAt(0) + "***" + parts[0].charAt(parts[0].length() - 1) : "***";
                String domain = parts[1].length() > 2 ?
                    parts[1].charAt(0) + "***" + parts[1].charAt(parts[1].length() - 1) : "***";
                return localPart + "@" + domain;
            }
        }

        return data.length() > 2 ? data.charAt(0) + "***" + data.charAt(data.length() - 1) : "***";
    }
}

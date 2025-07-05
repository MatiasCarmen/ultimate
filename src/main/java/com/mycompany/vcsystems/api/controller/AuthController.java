package com.mycompany.vcsystems.api.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import com.mycompany.vcsystems.modelo.service.UsuarioService;
import com.mycompany.vcsystems.security.JwtTokenProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:8081") // Permitir CORS para el frontend
public class AuthController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            return usuarioService.autenticarUsuario(request.getCorreo(), request.getContrasena())
                .map(user -> {
                    String token = jwtTokenProvider.createToken(user.getCorreo());
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error interno del servidor", "success", false));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            String newToken = usuarioService.refrescarToken(request.getOldToken());
            Map<String, Object> response = new HashMap<>();
            response.put("token", newToken);
            response.put("success", true);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Token inválido o expirado", "success", false));
        } catch (Exception e) {
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
}

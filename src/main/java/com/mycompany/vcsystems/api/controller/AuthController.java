package com.mycompany.vcsystems.api.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import com.mycompany.vcsystems.modelo.service.UsuarioService;
import com.mycompany.vcsystems.security.JwtTokenProvider;
import jakarta.validation.Valid;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        return usuarioService.autenticarUsuario(request.getCorreo(), request.getContrasena())
            .map(user -> {
                String token = jwtTokenProvider.createToken(user.getCorreo());
                Map<String, Object> response = new HashMap<>();
                response.put("token", token);

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
            .orElse(ResponseEntity.status(401).body(Map.of("error", "Credenciales inválidas")));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest request) {
        try {
            String newToken = usuarioService.refrescarToken(request.getOldToken());
            Map<String, String> response = new HashMap<>();
            response.put("token", newToken);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Token inválido o expirado");
        }
    }

    @Data
    static class LoginRequest {
        private String correo;
        private String contrasena;
    }

    @Data
    static class RefreshTokenRequest {
        private String oldToken;
    }
}

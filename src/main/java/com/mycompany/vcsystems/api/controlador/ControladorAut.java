package com.mycompany.vcsystems.api.controlador;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import com.mycompany.vcsystems.modelo.entidades.Usuario.Rol;
import com.mycompany.vcsystems.modelo.service.UsuarioService;
import com.mycompany.vcsystems.modelo.entidades.Usuario; 
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import org.springframework.dao.DataIntegrityViolationException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.ValidationException; 
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author MatiasCarmen
 */


@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:8081") // Puerto predifinido, lo tienen ocupado cambienlo
@Slf4j // Agregar para logging
public class ControladorAut {

    private final UsuarioService usuarioService;

    // Inyección por constructor para evitar dependencias circulares
    public ControladorAut(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            return usuarioService.autenticarUsuario(request.getCorreo(), request.getContrasena())
                    .map(user -> {
                        
                        Map<String, Object> response = new HashMap<>();
                        response.put("success", true);

                        // Agregar información del usuario que espera el frontend
                        Map<String, Object> usuario = new HashMap<>();
                        usuario.put("idUsuario", user.getIdUsuario());
                        usuario.put("nombre", user.getNombre());
                        usuario.put("correo", user.getCorreo());
                        usuario.put("rol", user.getRol().toString()); 
                        response.put("usuario", usuario);

                        // La redirección se manejará en el frontend basándose en la URL recibida
                        response.put("redirect", switch (user.getRol()) {
                            case GERENTE -> "/pages/gerente.html";
                            case TECNICO -> "/pages/tecnico.html";
                            case CLIENTE -> "/pages/cliente.html";
                            // solo existen estos roles,no agragar mas por favor 
                            default -> "/pages/login.html"; // Redirigir al login si el rol no es reconocido
                        });

                        return ResponseEntity.ok(response);
                    })
                    .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Credenciales inválidas", "success", false)));
        } catch (Exception e) {
            // Registrar el stack trace completo para depuración y monitoreo
            log.error("Error interno durante el proceso de login para usuario: {}",
                    request.getCorreo() != null ? maskSensitiveData(request.getCorreo()) : "unknown", e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno del servidor", "success", false));
        }
    }

    @PostMapping("/register/cliente")
    public ResponseEntity<?> registerCliente(@Valid @RequestBody RegisterClienteRequest request) {
        log.info("Intentando registrar nuevo cliente con correo: {}", request.getCorreo()); // Usando log.info y el correo

        try {
            Usuario nuevoUsuario = new Usuario();
            nuevoUsuario.setNombre(request.getNombre());
            nuevoUsuario.setCorreo(request.getCorreo());
            nuevoUsuario.setContrasena(request.getContrasena());
            nuevoUsuario.setRol(Rol.CLIENTE); // Establecer el rol como CLIENTE

            Usuario usuarioRegistrado = usuarioService.registrarUsuario(nuevoUsuario); // esto es para la creacion del cliente en la bd

            log.info("Cliente registrado exitosamente: {}", usuarioRegistrado.getCorreo()); // Log de éxito

            return ResponseEntity.status(HttpStatus.CREATED).body(usuarioRegistrado);
        } catch (ValidationException e) { 
            log.warn("Error de validación al registrar cliente: {}", e.getMessage()); // Log de validación
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage(), "success", false));
        } catch (Exception e) {
            if (e.getCause() instanceof DataIntegrityViolationException) {
                log.warn("Intento de registro de cliente con correo duplicado: {}", request.getCorreo()); 
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "El correo electrónico ya está registrado.", "success", false));
            }
            log.error("Error interno al registrar cliente", e); // Log de error genérico
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Error interno del servidor", "success", false));
        }
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

    @Data
    static class RegisterClienteRequest {
        @NotBlank(message = "El nombre es requerido")
        private String nombre;

        @Email(message = "Formato de email inválido")
        @NotBlank(message = "El correo es requerido")
        private String correo;

        @NotBlank(message = "La contraseña es requerida")
        private String contrasena; // La validación de formato de contraseña se hará en UsuarioService
    }
    /**
     * esto es la logica de las notificaciones
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

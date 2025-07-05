package com.mycompany.vcsystems.modelo.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.mycompany.vcsystems.modelo.repository.UsuarioRepository;
import com.mycompany.vcsystems.modelo.entidades.Usuario;
import com.mycompany.vcsystems.security.JwtTokenProvider;
import jakarta.validation.ValidationException;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder; // Usar inyección de dependencias en lugar de crear instancia

    private static final Pattern PASSWORD_PATTERN =
        Pattern.compile("^(?=.*[0-9])(?=.*[!@#$%^&*])(?=.*[a-zA-Z]).{8,}$");

    public Usuario registrarUsuario(Usuario usuario) {
        validarContrasena(usuario.getContrasena());
        usuario.setContrasena(passwordEncoder.encode(usuario.getContrasena()));
        return usuarioRepository.save(usuario);
    }

    private void validarContrasena(String password) {
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw new ValidationException("La contraseña debe tener al menos 8 caracteres, " +
                "incluir un número y un símbolo (!@#$%^&*)");
        }
    }

    public Optional<Usuario> autenticarUsuario(String correo, String rawPassword) {
        Optional<Usuario> userOpt = usuarioRepository.findByCorreo(correo);
        if (userOpt.isPresent() && passwordEncoder.matches(rawPassword, userOpt.get().getContrasena())) {
            return userOpt;
        }
        return Optional.empty();
    }

    public String obtenerRolPorCorreo(String correo) {
        return usuarioRepository.findByCorreo(correo)
            .map(usuario -> usuario.getRol().toString())
            .orElse(null);
    }

    public String refrescarToken(String oldToken) {
        if (jwtTokenProvider.validateToken(oldToken)) {
            String username = jwtTokenProvider.getUsernameFromToken(oldToken);
            return jwtTokenProvider.createToken(username);
        }
        throw new IllegalArgumentException("Token inválido o expirado");
    }
}

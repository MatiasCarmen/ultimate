package com.mycompany.vcsystems.modelo.service;

import org.springframework.stereotype.Service;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.annotation.Transactional;
import com.mycompany.vcsystems.modelo.repository.UsuarioRepository;
import com.mycompany.vcsystems.modelo.entidades.Usuario;
import jakarta.validation.ValidationException;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Transactional
public class UsuarioService implements UserDetailsService { // Add @Slf4j here if you prefer LOMBOK logging

    private final UsuarioRepository usuarioRepository;

    private static final Pattern PASSWORD_PATTERN =
        Pattern.compile("^(?=.*[0-9])(?=.*[!@#$%^&*])(?=.*[a-zA-Z]).{8,}$");

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class); // Manual Logger if no Lombok

    // Inyección por constructor para evitar dependencias circulares
    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Usuario registrarUsuario(Usuario usuario) {
        // Note: Password is NOT being encoded. For local testing ONLY!
        return usuarioRepository.save(usuario);
    }

    private void validarContrasena(String password) {
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw new ValidationException("La contraseña debe tener al menos 8 caracteres, " +
                "incluir un número y un símbolo (!@#$%^&*)");
        }
    }

    public Optional<Usuario> autenticarUsuario(String correo, String rawPassword) {
        log.info("Attempting authentication for user: {}", correo);
        // WARNING: Logging raw password is for temporary debugging ONLY. Remove in production!
        log.info("Raw password entered: {}", rawPassword); 

        Optional<Usuario> userOpt = usuarioRepository.findByCorreo(correo);
        boolean passwordMatches = userOpt.isPresent() && rawPassword.equals(userOpt.get().getContrasena()); // Direct comparison - INSECURE!

        log.info("Password match result for user {}: {}", correo, passwordMatches);
        if (passwordMatches) {
            log.info("Authentication successful for user: {}", correo);
            return userOpt;
        }
        return Optional.empty();
    }

    public String obtenerRolPorCorreo(String correo) {
        return usuarioRepository.findByCorreo(correo)
            .map(usuario -> usuario.getRol().toString())
            .orElse(null);
    }

    public Optional<Usuario> findByCorreo(String correo) {
        return usuarioRepository.findByCorreo(correo);
    }

    @Override
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
            .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + correo));

        return new org.springframework.security.core.userdetails.User(
            usuario.getCorreo(),
            usuario.getContrasena(),
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name()))
        );
    }
}

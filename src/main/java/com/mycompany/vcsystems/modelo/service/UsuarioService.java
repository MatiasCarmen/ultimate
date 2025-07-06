package com.mycompany.vcsystems.modelo.service;

import org.springframework.stereotype.Service;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.annotation.Transactional;
import com.mycompany.vcsystems.modelo.repository.UsuarioRepository;
import com.mycompany.vcsystems.modelo.entidades.Usuario;
import com.mycompany.vcsystems.modelo.repository.ClienteRepository;
import com.mycompany.vcsystems.modelo.entidades.Cliente;
import jakarta.validation.ValidationException;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.Collections;
import org.slf4j.Logger;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.slf4j.LoggerFactory;

@Service
@Transactional
public class UsuarioService implements UserDetailsService { // Add @Slf4j here if you prefer LOMBOK logging

    private final UsuarioRepository usuarioRepository;

    private final ClienteRepository clienteRepository;

    private final PasswordEncoder passwordEncoder;
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[0-9])(?=.*[!@#$%^&*])(?=.*[a-zA-Z]).{8,}$");

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class); // Manual Logger if no Lombok

    // Inyección por constructor para evitar dependencias circulares
    public UsuarioService(UsuarioRepository usuarioRepository, ClienteRepository clienteRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Usuario registrarUsuario(Usuario usuario) {
        validarContrasena(usuario.getContrasena()); // Add password validation

        usuario.setContrasena(passwordEncoder.encode(usuario.getContrasena()));
        // Assuming new users are active by default, you might set a flag or rely on default DB value
        // If Usuario entity has an 'activo' field and setter, uncomment the line below:
        // usuario.setActivo(true);
        Usuario savedUsuario = usuarioRepository.save(usuario);

        if (savedUsuario.getRol() == Usuario.Rol.CLIENTE) {
            Cliente cliente = new Cliente(); // Create Cliente using default constructor
            cliente.setUsuario(savedUsuario); // Associate the saved Usuario with the Cliente
            clienteRepository.save(cliente); // Save the associated Client entity
        }
        return savedUsuario; // Return the saved Usuario object
    }

    private void validarContrasena(String password) {
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw new ValidationException("La contraseña debe tener al menos 8 caracteres, " +
                    "incluir un número y un símbolo (!@#$%^&*)");
        }
    }

    public Optional<Usuario> autenticarUsuario(String correo, String rawPassword) {
        log.info("Attempting authentication for user: {}", correo);

        Optional<Usuario> userOpt = usuarioRepository.findByCorreo(correo);
        boolean passwordMatches = userOpt.isPresent() &&
                passwordEncoder.matches(rawPassword, userOpt.get().getContrasena());

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

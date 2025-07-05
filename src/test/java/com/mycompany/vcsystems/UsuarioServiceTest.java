package com.mycompany.vcsystems;

import com.mycompany.vcsystems.modelo.service.UsuarioService;
import com.mycompany.vcsystems.modelo.repository.UsuarioRepository;
import com.mycompany.vcsystems.modelo.entidades.Usuario;
import com.mycompany.vcsystems.modelo.entidades.Usuario.Rol;
import com.mycompany.vcsystems.security.JwtTokenProvider;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

@SpringBootTest(classes = VcsystemsApplication.class)
public class UsuarioServiceTest {

    @Autowired
    private UsuarioService usuarioService;

    @MockBean
    private UsuarioRepository usuarioRepository;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    public void setup() {
        Usuario mockUser = new Usuario();
        mockUser.setIdUsuario(1L);
        mockUser.setCorreo("test@example.com");
        mockUser.setContrasena("$2a$10$N9qo8uLOickgx2ZMRJWYNOeH6isr/DPWgHOGOgzVUeKRCqBXLyU6e"); // Hash real de Password123!
        mockUser.setNombre("Test User");
        mockUser.setRol(Rol.CLIENTE);

        // Asegúrate de que el hash sea el correcto
        Mockito.when(passwordEncoder.encode("Password123!"))
               .thenReturn("$2a$10$N9qo8uLOickgx2ZMRJWYNOeH6isr/DPWgHOGOgzVUeKRCqBXLyU6e");
        Mockito.when(passwordEncoder.matches("Password123!", "$2a$10$N9qo8uLOickgx2ZMRJWYNOeH6isr/DPWgHOGOgzVUeKRCqBXLyU6e"))
               .thenReturn(true);

        Mockito.when(usuarioRepository.save(Mockito.any(Usuario.class)))
               .thenReturn(mockUser);

        Mockito.when(usuarioRepository.findByCorreo("test@example.com"))
               .thenReturn(Optional.of(mockUser));
    }

    @Test
    public void testRegistrarYAutenticar() {
        Usuario u = new Usuario();
        u.setCorreo("test@example.com");
        u.setContrasena("Password123!");  // Contraseña que cumple requisitos: 8+ caracteres, número y símbolo
        u.setNombre("Test User");
        u.setRol(Rol.CLIENTE);

        Usuario saved = usuarioService.registrarUsuario(u);

        Assertions.assertNotNull(saved.getIdUsuario());
        Assertions.assertEquals("Test User", saved.getNombre());

        Optional<Usuario> auth = usuarioService.autenticarUsuario("test@example.com", "Password123!");
        Assertions.assertTrue(auth.isPresent());
        Assertions.assertEquals("Test User", auth.get().getNombre());

        Mockito.verify(usuarioRepository).save(Mockito.any(Usuario.class));
        Mockito.verify(usuarioRepository).findByCorreo("test@example.com");
    }
}

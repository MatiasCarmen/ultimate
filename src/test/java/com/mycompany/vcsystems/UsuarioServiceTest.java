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
        // Generar contraseña segura y hash BCrypt dinámicamente
        String securePassword = generateSecurePassword();
        String bcryptHash = generateBCryptHash(securePassword);

        Usuario mockUser = new Usuario();
        mockUser.setIdUsuario(1L);
        mockUser.setCorreo("test@example.com");
        mockUser.setContrasena(bcryptHash); // Hash generado dinámicamente
        mockUser.setNombre("Test User");
        mockUser.setRol(Rol.CLIENTE);

        // Configurar el mock del passwordEncoder con valores dinámicos
        Mockito.when(passwordEncoder.encode(securePassword))
               .thenReturn(bcryptHash);
        Mockito.when(passwordEncoder.matches(securePassword, bcryptHash))
               .thenReturn(true);

        Mockito.when(usuarioRepository.save(Mockito.any(Usuario.class)))
               .thenReturn(mockUser);

        Mockito.when(usuarioRepository.findByCorreo("test@example.com"))
               .thenReturn(Optional.of(mockUser));

        // Almacenar para uso en el test
        this.testPassword = securePassword;
    }

    private String testPassword; // Campo para almacenar la contraseña generada

    @Test
    public void testRegistrarYAutenticar() {
        Usuario u = new Usuario();
        u.setCorreo("test@example.com");
        u.setContrasena(testPassword); // Usar contraseña generada dinámicamente
        u.setNombre("Test User");
        u.setRol(Rol.CLIENTE);

        Usuario saved = usuarioService.registrarUsuario(u);

        Assertions.assertNotNull(saved.getIdUsuario());
        Assertions.assertEquals("Test User", saved.getNombre());

        Optional<Usuario> auth = usuarioService.autenticarUsuario("test@example.com", testPassword);
        Assertions.assertTrue(auth.isPresent());
        Assertions.assertEquals("Test User", auth.get().getNombre());

        Mockito.verify(usuarioRepository).save(Mockito.any(Usuario.class));
        Mockito.verify(usuarioRepository).findByCorreo("test@example.com");
    }

    /**
     * Genera una contraseña segura dinámicamente para las pruebas
     */
    private String generateSecurePassword() {
        // Generar contraseña segura con caracteres aleatorios
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        StringBuilder password = new StringBuilder();

        // Asegurar que tenga al menos una mayúscula, minúscula, número y símbolo
        password.append("T"); // Mayúscula
        password.append("e"); // Minúscula
        password.append("5"); // Número
        password.append("!"); // Símbolo

        // Completar con caracteres aleatorios hasta 12 caracteres
        java.util.Random random = new java.util.Random();
        for (int i = 4; i < 12; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }

        // Mezclar los caracteres para que no sean predecibles
        return shuffleString(password.toString());
    }

    /**
     * Genera un hash BCrypt real para la contraseña de prueba
     */
    private String generateBCryptHash(String password) {
        // Usar un encoder real para generar el hash
        org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder =
            new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        return encoder.encode(password);
    }

    /**
     * Mezcla los caracteres de una cadena aleatoriamente
     */
    private String shuffleString(String input) {
        java.util.List<Character> characters = new java.util.ArrayList<>();
        for (char c : input.toCharArray()) {
            characters.add(c);
        }
        java.util.Collections.shuffle(characters);

        StringBuilder result = new StringBuilder();
        for (char c : characters) {
            result.append(c);
        }
        return result.toString();
    }
}

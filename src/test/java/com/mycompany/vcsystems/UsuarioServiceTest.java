package com.mycompany.vcsystems;

import com.mycompany.vcsystems.modelo.service.UsuarioService;
import com.mycompany.vcsystems.modelo.repository.UsuarioRepository;
import com.mycompany.vcsystems.modelo.entidades.Usuario;
import com.mycompany.vcsystems.modelo.entidades.Usuario.Rol;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Optional;

@SpringBootTest(classes = VcsystemsApplication.class)
/**
 *
 * @author MatiasCarmen
 */
public class UsuarioServiceTest {

    @Autowired
    private UsuarioService usuarioService;

    @MockBean
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    public void setup() {
        Usuario mockUser = new Usuario();
        mockUser.setIdUsuario(1L);
        mockUser.setCorreo("test@example.com");
        mockUser.setContrasena("testpassword"); // Contraseña en texto plano para la prueba
        mockUser.setNombre("Test User");
        mockUser.setRol(Rol.CLIENTE);

        Mockito.when(usuarioRepository.save(Mockito.any(Usuario.class)))
               .thenReturn(mockUser);

        Mockito.when(usuarioRepository.findByCorreo("test@example.com"))
               .thenReturn(Optional.of(mockUser));
    }

    @Test
    public void testRegistrarYAutenticar() {
        Usuario u = new Usuario();
        Optional<Usuario> auth = usuarioService.autenticarUsuario("test@example.com", "testpassword");
        Assertions.assertTrue(auth.isPresent());
        Assertions.assertEquals("Test User", auth.get().getNombre());

        Mockito.verify(usuarioRepository).save(Mockito.any(Usuario.class));
        Mockito.verify(usuarioRepository).findByCorreo("test@example.com");
    }

}

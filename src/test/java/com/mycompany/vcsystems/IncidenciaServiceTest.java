package com.mycompany.vcsystems;

import com.mycompany.vcsystems.modelo.service.IncidenciaService;
import com.mycompany.vcsystems.modelo.service.IncidenciaOperationResult;
import com.mycompany.vcsystems.modelo.repository.IncidenciaRepository;
import com.mycompany.vcsystems.modelo.repository.ClienteRepository;
import com.mycompany.vcsystems.modelo.service.UsuarioService;
import com.mycompany.vcsystems.modelo.service.NotificacionService;
import com.mycompany.vcsystems.modelo.entidades.Incidencia;
import com.mycompany.vcsystems.modelo.entidades.Usuario;
import com.mycompany.vcsystems.modelo.entidades.Cliente;
import com.mycompany.vcsystems.modelo.entidades.Usuario.Rol;
import com.mycompany.vcsystems.modelo.entidades.Incidencia.Estado;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.annotation.Rollback;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Optional;
/**
 *
 * @author MatiasCarmen
 */
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = VcsystemsApplication.class)
@Transactional
@Rollback
public class IncidenciaServiceTest {

    @Autowired
    private IncidenciaService incidenciaService;

    @Autowired
    private IncidenciaRepository incidenciaRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private UsuarioService usuarioService;

    @MockBean
    private NotificacionService notificacionService; // Mockear el servicio de notificaciones

    private Usuario clienteUser ;
    private Cliente cliente;
    private Usuario tecnicoUser ;

    @BeforeEach
    public void setup() {
        // Crear usuarios con contraseñas seguras generadas dinámicamente
        String securePassword = generateSecurePassword();

        clienteUser  = new Usuario();
        clienteUser .setCorreo("cli@example.com");
        clienteUser .setContrasena(securePassword);
        clienteUser .setNombre("Cliente");
        clienteUser .setRol(Rol.CLIENTE);
        clienteUser  = usuarioService.registrarUsuario(clienteUser );

        cliente = new Cliente();
        cliente.setUsuario(clienteUser );
        cliente.setNombreEmpresa("Empresa X");
        cliente.setDireccionEmpresa("Dir X");
        cliente = clienteRepository.save(cliente);

        tecnicoUser  = new Usuario();
        tecnicoUser .setCorreo("tec@example.com");
        tecnicoUser .setContrasena(generateSecurePassword());
        tecnicoUser .setNombre("Tecnico");
        tecnicoUser .setRol(Rol.TECNICO);
        tecnicoUser  = usuarioService.registrarUsuario(tecnicoUser );
    }

    @Test
    public void testCrearAsignarYCambiarEstado() {
        // Crear una nueva incidencia
        Incidencia inc = new Incidencia();
        inc.setCliente(cliente);
        inc.setDescripcion("Desc");
        inc.setEstado(Estado.PENDIENTE);
        
        // Guardar la incidencia
        Incidencia saved = incidenciaService.crearIncidencia(inc);
        assertNotNull(saved.getIdIncidencia(), "La incidencia no se guardó correctamente.");

        // Asignar técnico
        IncidenciaOperationResult assignResult = incidenciaService.asignarTecnico(saved.getIdIncidencia(), tecnicoUser );
        assertTrue(assignResult.isSuccess(), "No se pudo asignar el técnico.");
        assertEquals(tecnicoUser .getIdUsuario(), assignResult.getIncidencia().getTecnico().getIdUsuario(), "El técnico asignado no es el esperado.");

        // Cambiar estado a RESUELTA
        IncidenciaOperationResult stateResult = incidenciaService.cambiarEstado(saved.getIdIncidencia(), Estado.RESUELTA);
        assertTrue(stateResult.isSuccess(), "No se pudo cambiar el estado de la incidencia.");
        assertEquals(Estado.RESUELTA, stateResult.getIncidencia().getEstado(), "El estado de la incidencia no se cambió correctamente.");
    }

    private String generateSecurePassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        StringBuilder password = new StringBuilder();
        password.append("A"); // Mayúscula
        password.append("a"); // Minúscula
        password.append("1"); // Número
        password.append("!"); // Símbolo

        java.util.Random random = new java.util.Random();
        for (int i = 4; i < 12; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }

        return shuffleString(password.toString());
    }

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

package com.mycompany.vcsystems;

import com.mycompany.vcsystems.modelo.service.IncidenciaService;
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

    private Usuario clienteUser;
    private Cliente cliente;
    private Usuario tecnicoUser;

    @BeforeEach
    public void setup() {
        // No necesitamos deleteAll() ya que @Transactional + @Rollback se encarga de limpiar
        // crear usuarios
        clienteUser = new Usuario();
        clienteUser.setCorreo("cli@example.com");
        clienteUser.setContrasena("Password123!"); // Contraseña que cumple requisitos
        clienteUser.setNombre("Cliente");
        clienteUser.setRol(Rol.CLIENTE);
        clienteUser = usuarioService.registrarUsuario(clienteUser);

        cliente = new Cliente();
        cliente.setUsuario(clienteUser);
        cliente.setNombreEmpresa("Empresa X");
        cliente.setDireccionEmpresa("Dir X");
        cliente = clienteRepository.save(cliente);

        tecnicoUser = new Usuario();
        tecnicoUser.setCorreo("tec@example.com");
        tecnicoUser.setContrasena("Password123!"); // También corregir esta contraseña
        tecnicoUser.setNombre("Tecnico");
        tecnicoUser.setRol(Rol.TECNICO);
        tecnicoUser = usuarioService.registrarUsuario(tecnicoUser);
    }

    @Test
    public void testCrearAsignarYCambiarEstado() {
        Incidencia inc = new Incidencia();
        inc.setCliente(cliente);
        inc.setDescripcion("Desc");
        inc.setEstado(Estado.PENDIENTE);
        Incidencia saved = incidenciaService.crearIncidencia(inc);
        assertNotNull(saved.getIdIncidencia());
        // asignar tecnico
        Optional<Incidencia> optAssigned = incidenciaService.asignarTecnico(saved.getIdIncidencia(), tecnicoUser);
        assertTrue(optAssigned.isPresent());
        assertEquals(tecnicoUser.getIdUsuario(), optAssigned.get().getTecnico().getIdUsuario());
        // cambiar estado
        Optional<Incidencia> optState = incidenciaService.cambiarEstado(saved.getIdIncidencia(), Estado.RESUELTA);
        assertTrue(optState.isPresent());
        assertEquals(Estado.RESUELTA, optState.get().getEstado());
    }
}

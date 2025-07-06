package com.mycompany.vcsystems;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.vcsystems.modelo.entidades.Incidencia;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.mycompany.vcsystems.modelo.service.IncidenciaService;
import org.mockito.Mockito;
import java.util.Collections;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest(classes = VcsystemsApplication.class)
@AutoConfigureMockMvc
/**
 *
 * @author MatiasCarmen
 */
public class IncidenciaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IncidenciaService incidenciaService;

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testGetAllIncidencias() throws Exception {
        // Mock del servicio para devolver una lista vacía
        Mockito.when(incidenciaService.listAll()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/incidencias")
               .accept(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(content().json("[]")); // Esperamos una lista vacía
    }

    @Test
    @WithMockUser(username = "cliente", roles = {"CLIENTE"})
    public void testCreateIncidenciaWithValidationError() throws Exception {
        // NO configurar ningún mock del servicio para que la validación real ocurra

        // Crear un objeto Incidencia inválido SIN cliente asignado para forzar error de validación
        Incidencia incInvalida = new Incidencia();
        incInvalida.setDescripcion(""); // Descripción vacía para forzar error @NotBlank
        // NO asignamos cliente (será null) para forzar error de validación en el servicio
        // NO asignamos estado, falla, etc.

        // Agregar logs de depuración
        String jsonContent = objectMapper.writeValueAsString(incInvalida);
        System.out.println("Contenido enviado: " + jsonContent);

        var result = mockMvc.perform(post("/api/incidencias")
               .contentType(MediaType.APPLICATION_JSON)
               .content(jsonContent)
               .with(csrf()));

        System.out.println("Respuesta recibida: " + result.andReturn().getResponse().getContentAsString());
        System.out.println("Status recibido: " + result.andReturn().getResponse().getStatus());

        result.andExpect(status().isBadRequest());
    }
}

package com.mycompany.vcsystems.modelo.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.mycompany.vcsystems.modelo.repository.IncidenciaRepository;
import com.mycompany.vcsystems.modelo.entidades.Incidencia;
import com.mycompany.vcsystems.modelo.entidades.Usuario;
import jakarta.validation.ValidationException;
import java.util.Optional;
import java.util.List;

@Service
public class IncidenciaService {

    @Autowired
    private IncidenciaRepository incidenciaRepository;

    @Autowired
    private NotificacionService notificacionService;

    public Incidencia crearIncidencia(Incidencia incidencia) {
        if (incidencia.getCliente() == null) {
            throw new ValidationException("La incidencia debe tener un cliente asignado");
        }

        incidencia.setEstado(Incidencia.Estado.PENDIENTE);
        Incidencia savedIncidencia = incidenciaRepository.save(incidencia);

        // Notificar a los administradores sobre la nueva incidencia
        notificacionService.notificarIncidencia(
            "admin@vcsystems.com",
            "Nueva Incidencia Creada",
            String.format("Se ha creado la incidencia #%d para el cliente %s",
                savedIncidencia.getIdIncidencia(),
                savedIncidencia.getCliente().getNombreEmpresa())
        );

        return savedIncidencia;
    }

    public Optional<Incidencia> asignarTecnico(Long idIncidencia, Usuario tecnico) {
        if (!tecnico.getRol().equals(Usuario.Rol.TECNICO)) {
            throw new ValidationException("El usuario asignado debe ser un técnico");
        }

        Optional<Incidencia> opt = incidenciaRepository.findById(idIncidencia);
        if (opt.isPresent()) {
            Incidencia inc = opt.get();
            inc.setTecnico(tecnico);
            inc.setEstado(Incidencia.Estado.EN_PROCESO);

            Incidencia updated = incidenciaRepository.save(inc);

            // Notificar al técnico asignado
            notificacionService.notificarIncidencia(
                tecnico.getCorreo(),
                "Nueva Incidencia Asignada",
                String.format("Se te ha asignado la incidencia #%d", idIncidencia)
            );

            return Optional.of(updated);
        }
        return Optional.empty();
    }

    public Optional<Incidencia> cambiarEstado(Long idIncidencia, Incidencia.Estado nuevoEstado) {
        Optional<Incidencia> opt = incidenciaRepository.findById(idIncidencia);
        if (opt.isPresent()) {
            Incidencia inc = opt.get();
            inc.setEstado(nuevoEstado);

            Incidencia updated = incidenciaRepository.save(inc);

            // Notificar cambio de estado
            if (inc.getCliente() != null && inc.getCliente().getUsuario() != null) {
                notificacionService.notificarIncidencia(
                    inc.getCliente().getUsuario().getCorreo(),
                    "Actualización de Incidencia",
                    String.format("Su incidencia #%d ha cambiado a estado: %s",
                        idIncidencia, nuevoEstado)
                );
            }

            return Optional.of(updated);
        }
        return Optional.empty();
    }

    public List<Incidencia> listarPorEstado(String estado) {
        return incidenciaRepository.findByEstado(estado);
    }

    public List<Incidencia> listarPorTecnico(Long idTecnico) {
        return incidenciaRepository.findByTecnico_IdUsuario(idTecnico);
    }

    public List<Incidencia> listAll() {
        return incidenciaRepository.findAll();
    }
}

package com.mycompany.vcsystems.modelo.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import com.mycompany.vcsystems.modelo.repository.IncidenciaRepository;
import com.mycompany.vcsystems.modelo.entidades.Incidencia;
import com.mycompany.vcsystems.modelo.entidades.Usuario;
import com.mycompany.vcsystems.modelo.events.IncidenciaCreatedEvent;
import com.mycompany.vcsystems.modelo.events.IncidenciaTecnicoAssignedEvent;
import com.mycompany.vcsystems.modelo.events.IncidenciaStatusChangedEvent;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import java.util.Optional;
import java.util.List;

@Service
@Slf4j
public class IncidenciaService {

    @Autowired
    private IncidenciaRepository incidenciaRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    public Incidencia crearIncidencia(Incidencia incidencia) {
        validateIncidenciaForCreation(incidencia);
        incidencia.setEstado(Incidencia.Estado.PENDIENTE);
        Incidencia savedIncidencia = incidenciaRepository.save(incidencia);
        log.info("Incidencia creada exitosamente con ID: {}", savedIncidencia.getIdIncidencia());
        eventPublisher.publishEvent(new IncidenciaCreatedEvent(this, savedIncidencia));
        return savedIncidencia;
    }

    public IncidenciaOperationResult asignarTecnico(Long idIncidencia, Usuario tecnico) {
        if (tecnico == null) {
            return IncidenciaOperationResult.businessError("El técnico es requerido");
        }

        if (!tecnico.getRol().equals(Usuario.Rol.TECNICO)) {
            return IncidenciaOperationResult.businessError("El usuario asignado debe tener rol de técnico");
        }

        Optional<Incidencia> incidenciaOpt = incidenciaRepository.findById(idIncidencia);
        if (incidenciaOpt.isEmpty()) {
            return IncidenciaOperationResult.notFound("No se encontró la incidencia con ID: " + idIncidencia);
        }

        Incidencia incidencia = incidenciaOpt.get();
        if (incidencia.getTecnico() != null) {
            return IncidenciaOperationResult.businessError("La incidencia ya tiene un técnico asignado: " + incidencia.getTecnico().getNombre());
        }

        if (incidencia.getEstado() == Incidencia.Estado.CERRADA || incidencia.getEstado() == Incidencia.Estado.RESUELTA) {
            return IncidenciaOperationResult.businessError("No se puede asignar técnico a una incidencia en estado: " + incidencia.getEstado());
        }

        incidencia.setTecnico(tecnico);
        incidencia.setEstado(Incidencia.Estado.ASIGNADA);
        Incidencia updatedIncidencia = incidenciaRepository.save(incidencia);
        log.info("Técnico {} asignado exitosamente a incidencia {}", tecnico.getNombre(), idIncidencia);
        eventPublisher.publishEvent(new IncidenciaTecnicoAssignedEvent(this, updatedIncidencia, tecnico));
        return IncidenciaOperationResult.success(updatedIncidencia);
    }

    public IncidenciaOperationResult cambiarEstado(Long idIncidencia, Incidencia.Estado nuevoEstado) {
        if (nuevoEstado == null) {
            return IncidenciaOperationResult.businessError("El nuevo estado es requerido");
        }

        Optional<Incidencia> incidenciaOpt = incidenciaRepository.findById(idIncidencia);
        if (incidenciaOpt.isEmpty()) {
            return IncidenciaOperationResult.notFound("No se encontró la incidencia con ID: " + idIncidencia);
        }

        Incidencia incidencia = incidenciaOpt.get();
        Incidencia.Estado estadoAnterior = incidencia.getEstado();

        // Log para verificar el estado anterior y el nuevo estado
        log.info("Intentando cambiar estado de incidencia {} de {} a {}", idIncidencia, estadoAnterior, nuevoEstado);

        if (!isValidStateTransition(estadoAnterior, nuevoEstado)) {
            return IncidenciaOperationResult.businessError(String.format("Transición de estado inválida: %s -> %s", estadoAnterior, nuevoEstado));
        }

        if (nuevoEstado == Incidencia.Estado.EN_PROCESO && incidencia.getTecnico() == null) {
            return IncidenciaOperationResult.businessError("No se puede cambiar a EN_PROCESO sin tener un técnico asignado");
        }

        // Realizar cambio de estado
        incidencia.setEstado(nuevoEstado);
        Incidencia updatedIncidencia = incidenciaRepository.save(incidencia);
        log.info("Estado de incidencia {} cambiado de {} a {}", idIncidencia, estadoAnterior, nuevoEstado);
        eventPublisher.publishEvent(new IncidenciaStatusChangedEvent(this, updatedIncidencia, estadoAnterior, nuevoEstado));
        return IncidenciaOperationResult.success(updatedIncidencia);
    }

    private void validateIncidenciaForCreation(Incidencia incidencia) {
        if (incidencia == null) {
            throw new ValidationException("La incidencia no puede ser nula");
        }

        if (incidencia.getCliente() == null) {
            throw new ValidationException("La incidencia debe tener un cliente asignado");
        }

        if (incidencia.getDescripcion() == null || incidencia.getDescripcion().trim().isEmpty()) {
            throw new ValidationException("La incidencia debe tener una descripción");
        }

        if (incidencia.getDescripcion().length() > 1000) {
            throw new ValidationException("La descripción no puede exceder 1000 caracteres");
        }
    }

    private boolean isValidStateTransition(Incidencia.Estado from, Incidencia.Estado to) {
        if (from == to) {
            return false; // No se permite cambiar al mismo estado
        }

        return switch (from) {
            case PENDIENTE -> to == Incidencia.Estado.ASIGNADA || to == Incidencia.Estado.CERRADA;
            case ASIGNADA -> to == Incidencia.Estado.EN_PROCESO || to == Incidencia.Estado.PENDIENTE || to == Incidencia.Estado.CERRADA || to == Incidencia.Estado.RESUELTA;
            case EN_PROCESO -> to == Incidencia.Estado.RESUELTA || to == Incidencia.Estado.ASIGNADA || to == Incidencia.Estado.CERRADA;
            case RESUELTA -> to == Incidencia.Estado.CERRADA || to == Incidencia.Estado.EN_PROCESO;
            case CERRADA -> false; // Las incidencias cerradas no pueden cambiar de estado
        };
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

    public IncidenciaOperationResult buscarPorId(Long idIncidencia) {
        if (idIncidencia == null) {
            return IncidenciaOperationResult.businessError("El ID de incidencia es requerido");
        }

        Optional<Incidencia> incidenciaOpt = incidenciaRepository.findById(idIncidencia);
        if (incidenciaOpt.isEmpty()) {
            return IncidenciaOperationResult.notFound("No se encontró la incidencia con ID: " + idIncidencia);
        }

        return IncidenciaOperationResult.success(incidenciaOpt.get());
    }

    public IncidenciaStatistics getStatistics() {
        List<Incidencia> todasLasIncidencias = incidenciaRepository.findAll();

        long totalIncidencias = todasLasIncidencias.size();
        long pendientes = todasLasIncidencias.stream()
            .filter(i -> i.getEstado() == Incidencia.Estado.PENDIENTE)
            .count();
        long asignadas = todasLasIncidencias.stream()
            .filter(i -> i.getEstado() == Incidencia.Estado.ASIGNADA)
            .count();
        long enProceso = todasLasIncidencias.stream()
            .filter(i -> i.getEstado() == Incidencia.Estado.EN_PROCESO)
            .count();
        long resueltas = todasLasIncidencias.stream()
            .filter(i -> i.getEstado() == Incidencia.Estado.RESUELTA)
            .count();
        long cerradas = todasLasIncidencias.stream()
            .filter(i -> i.getEstado() == Incidencia.Estado.CERRADA)
            .count();

        return new IncidenciaStatistics(totalIncidencias, pendientes, asignadas, enProceso, resueltas, cerradas);
    }

    public static class IncidenciaStatistics {
        private final long total;
        private final long pendientes;
        private final long asignadas;
        private final long enProceso;
        private final long resueltas;
        private final long cerradas;

        public IncidenciaStatistics(long total, long pendientes, long asignadas,
                                  long enProceso, long resueltas, long cerradas) {
            this.total = total;
            this.pendientes = pendientes;
            this.asignadas = asignadas;
            this.enProceso = enProceso;
            this.resueltas = resueltas;
            this.cerradas = cerradas;
        }

        // Getters
        public long getTotal() { return total; }
        public long getPendientes() { return pendientes; }
        public long getAsignadas() { return asignadas; }
        public long getEnProceso() { return enProceso; }
        public long getResueltas() { return resueltas; }
        public long getCerradas() { return cerradas; }
    }
}

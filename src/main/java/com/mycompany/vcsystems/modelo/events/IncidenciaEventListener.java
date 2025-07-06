package com.mycompany.vcsystems.modelo.events;

import com.mycompany.vcsystems.modelo.service.NotificacionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listener que maneja los eventos de incidencias y envía notificaciones
 * de forma desacoplada del servicio principal
 */
@Component
@Slf4j
/**
 *
 * @author MatiasCarmen
 */
public class IncidenciaEventListener {

    @Autowired
    private NotificacionService notificacionService;

    /**
     * Maneja el evento de creación de incidencia
     */
    @EventListener
    @Async
    public void handleIncidenciaCreated(IncidenciaCreatedEvent event) {
        try {
            log.info("Procesando evento de incidencia creada: {}", event.getIncidencia().getIdIncidencia());

            notificacionService.notificarIncidencia(
                event.getNotificationRecipient(),
                event.getNotificationSubject(),
                event.getNotificationMessage()
            );

            log.debug("Notificación enviada exitosamente para incidencia creada: {}",
                event.getIncidencia().getIdIncidencia());

        } catch (Exception e) {
            log.error("Error enviando notificación para incidencia creada: {}",
                event.getIncidencia().getIdIncidencia(), e);
        }
    }

    /**
     * Maneja el evento de asignación de técnico
     */
    @EventListener
    @Async
    public void handleTecnicoAssigned(IncidenciaTecnicoAssignedEvent event) {
        try {
            log.info("Procesando evento de técnico asignado para incidencia: {}",
                event.getIncidencia().getIdIncidencia());

            notificacionService.notificarIncidencia(
                event.getNotificationRecipient(),
                event.getNotificationSubject(),
                event.getNotificationMessage()
            );

            log.debug("Notificación de asignación enviada exitosamente a técnico: {}",
                event.getTecnico().getCorreo());

        } catch (Exception e) {
            log.error("Error enviando notificación de asignación para incidencia: {}",
                event.getIncidencia().getIdIncidencia(), e);
        }
    }

    /**
     * Maneja el evento de cambio de estado
     */
    @EventListener
    @Async
    public void handleStatusChanged(IncidenciaStatusChangedEvent event) {
        try {
            String recipient = event.getNotificationRecipient();
            if (recipient == null) {
                log.warn("No se encontró destinatario para notificación de cambio de estado de incidencia: {}",
                    event.getIncidencia().getIdIncidencia());
                return;
            }

            log.info("Procesando evento de cambio de estado para incidencia: {} de {} a {}",
                event.getIncidencia().getIdIncidencia(),
                event.getEstadoAnterior(),
                event.getNuevoEstado());

            notificacionService.notificarIncidencia(
                recipient,
                event.getNotificationSubject(),
                event.getNotificationMessage()
            );

            log.debug("Notificación de cambio de estado enviada exitosamente");

        } catch (Exception e) {
            log.error("Error enviando notificación de cambio de estado para incidencia: {}",
                event.getIncidencia().getIdIncidencia(), e);
        }
    }
}

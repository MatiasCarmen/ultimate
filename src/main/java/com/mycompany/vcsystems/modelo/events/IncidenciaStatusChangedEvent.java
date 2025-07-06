package com.mycompany.vcsystems.modelo.events;

import com.mycompany.vcsystems.modelo.entidades.Incidencia;

/**
 * Evento disparado cuando cambia el estado de una incidencia
 */
/**
 *
 * @author MatiasCarmen
 */
public class IncidenciaStatusChangedEvent extends IncidenciaEvent {

    private final Incidencia.Estado estadoAnterior;
    private final Incidencia.Estado nuevoEstado;

    public IncidenciaStatusChangedEvent(Object source, Incidencia incidencia,
                                       Incidencia.Estado estadoAnterior, Incidencia.Estado nuevoEstado) {
        super(source, incidencia, "STATUS_CHANGED");
        this.estadoAnterior = estadoAnterior;
        this.nuevoEstado = nuevoEstado;
    }

    public Incidencia.Estado getEstadoAnterior() {
        return estadoAnterior;
    }

    public Incidencia.Estado getNuevoEstado() {
        return nuevoEstado;
    }

    public String getNotificationMessage() {
        return String.format("Su incidencia #%d ha cambiado a estado: %s",
            getIncidencia().getIdIncidencia(), nuevoEstado);
    }

    public String getNotificationRecipient() {
        // Notificar al cliente propietario de la incidencia
        if (getIncidencia().getCliente() != null &&
            getIncidencia().getCliente().getUsuario() != null) {
            return getIncidencia().getCliente().getUsuario().getCorreo();
        }
        return null;
    }

    public String getNotificationSubject() {
        return "Actualización de Incidencia";
    }
}

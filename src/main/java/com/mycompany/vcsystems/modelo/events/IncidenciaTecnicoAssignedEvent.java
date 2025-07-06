package com.mycompany.vcsystems.modelo.events;

import com.mycompany.vcsystems.modelo.entidades.Incidencia;
import com.mycompany.vcsystems.modelo.entidades.Usuario;

/**
 * Evento disparado cuando se asigna un técnico a una incidencia
 */
/**
 *
 * @author MatiasCarmen
 */
public class IncidenciaTecnicoAssignedEvent extends IncidenciaEvent {

    private final Usuario tecnico;

    public IncidenciaTecnicoAssignedEvent(Object source, Incidencia incidencia, Usuario tecnico) {
        super(source, incidencia, "TECNICO_ASSIGNED");
        this.tecnico = tecnico;
    }

    public Usuario getTecnico() {
        return tecnico;
    }

    public String getNotificationMessage() {
        return String.format("Se te ha asignado la incidencia #%d",
            getIncidencia().getIdIncidencia());
    }

    public String getNotificationRecipient() {
        return tecnico.getCorreo();
    }

    public String getNotificationSubject() {
        return "Nueva Incidencia Asignada";
    }
}

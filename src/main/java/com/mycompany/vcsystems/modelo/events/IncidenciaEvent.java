package com.mycompany.vcsystems.modelo.events;

import com.mycompany.vcsystems.modelo.entidades.Incidencia;
import org.springframework.context.ApplicationEvent;


// Evento base para todas las operaciones relacionadas con incidencias

/**
 *
 * @author MatiasCarmen
 */
public abstract class IncidenciaEvent extends ApplicationEvent {

    private final Incidencia incidencia;
    private final String action;

    public IncidenciaEvent(Object source, Incidencia incidencia, String action) {
        super(source);
        this.incidencia = incidencia;
        this.action = action;
    }

    public Incidencia getIncidencia() {
        return incidencia;
    }

    public String getAction() {
        return action;
    }
}

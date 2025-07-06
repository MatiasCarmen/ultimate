package com.mycompany.vcsystems.modelo.events;

import com.mycompany.vcsystems.modelo.entidades.Incidencia;


//Evento disparado cuando se crea una nueva incidencia
 

/**
 *
 * @author MatiasCarmen
 */
public class IncidenciaCreatedEvent extends IncidenciaEvent {

    public IncidenciaCreatedEvent(Object source, Incidencia incidencia) {
        super(source, incidencia, "CREATED");
    }

    
      // Obtiene el mensaje de notificación para el evento
     
    public String getNotificationMessage() {
        return String.format("Se ha creado la incidencia #%d para el cliente %s",
            getIncidencia().getIdIncidencia(),
            getIncidencia().getCliente().getNombreEmpresa());
    }

    
     //Obtiene el destinatario de la notificación
     
    public String getNotificationRecipient() {
        return "admin@vcsystems.com"; // Administradores por defecto
    }

   
     //Obtiene el asunto de la notificación
    
    public String getNotificationSubject() {
        return "Nueva Incidencia Creada";
    }
}

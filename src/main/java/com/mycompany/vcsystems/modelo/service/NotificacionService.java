package com.mycompany.vcsystems.modelo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class NotificacionService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private JavaMailSender emailSender;

    /**
     * Simula el envío de notificaciones. Por ahora solo imprime o registra el evento.
     */
    public void enviarNotificacion(String destinatario, String mensaje) {
        // Enviar por WebSocket al topic de incidencias
        messagingTemplate.convertAndSend("/topic/incidencias", mensaje);

        // También registra en logs
        log.info("Notificación WS enviada a {}: {}", destinatario, mensaje);
    }

    public void enviarNotificacionEmail(String destinatario, String asunto, String contenido) {
        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(contenido, true); // true indica que el contenido es HTML

            emailSender.send(message);
            log.info("Email enviado a: {}", destinatario);
        } catch (MessagingException e) {
            log.error("Error enviando email a {}: {}", destinatario, e.getMessage());
            throw new RuntimeException("Error enviando email", e);
        }
    }

    public void notificarIncidencia(String destinatario, String asunto, String mensaje) {
        // Envía tanto por WebSocket como por email
        enviarNotificacion(destinatario, mensaje);
        enviarNotificacionEmail(destinatario, asunto, mensaje);
    }
}

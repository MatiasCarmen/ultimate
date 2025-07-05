package com.mycompany.vcsystems.modelo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
@Slf4j
public class NotificacionService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private JavaMailSender emailSender;

    @Value("${app.environment:development}")
    private String environment;

    /**
     * Simula el envío de notificaciones. Por ahora solo imprime o registra el evento.
     */
    public void enviarNotificacion(String destinatario, String mensaje) {
        // Enviar por WebSocket al topic de incidencias
        messagingTemplate.convertAndSend("/topic/incidencias", mensaje);

        // Registra en logs con datos protegidos
        log.info("Notificación WS enviada a {}: {}",
            maskSensitiveData(destinatario),
            truncateMessage(mensaje));
    }

    public void enviarNotificacionEmail(String destinatario, String asunto, String contenido) {
        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(contenido, true); // true indica que el contenido es HTML

            emailSender.send(message);

            // Log seguro sin exponer email completo
            log.info("Email enviado exitosamente a: {}", maskSensitiveData(destinatario));
        } catch (MessagingException e) {
            log.error("Error enviando email a {}: {}",
                maskSensitiveData(destinatario),
                e.getMessage());
            throw new RuntimeException("Error enviando email", e);
        }
    }

    public void notificarIncidencia(String destinatario, String asunto, String mensaje) {
        // Envía tanto por WebSocket como por email
        enviarNotificacion(destinatario, mensaje);
        enviarNotificacionEmail(destinatario, asunto, mensaje);
    }

    /**
     * Enmascara datos sensibles como emails para logs seguros
     * Ejemplo: usuario@dominio.com -> u****o@d****o.com
     */
    private String maskSensitiveData(String data) {
        if (data == null || data.isEmpty()) {
            return "***";
        }

        // En producción, usar enmascaramiento más fuerte
        if ("production".equalsIgnoreCase(environment)) {
            return generateHash(data);
        }

        // En desarrollo, usar enmascaramiento parcial
        if (data.contains("@")) {
            // Es un email
            String[] parts = data.split("@");
            if (parts.length == 2) {
                String localPart = maskString(parts[0]);
                String domain = maskString(parts[1]);
                return localPart + "@" + domain;
            }
        }

        // Para otros tipos de datos
        return maskString(data);
    }

    /**
     * Enmascara una cadena manteniendo solo el primer y último carácter
     */
    private String maskString(String str) {
        if (str.length() <= 2) {
            return "***";
        }
        int maskLength = Math.max(4, str.length() - 2);
        return str.charAt(0) + "*".repeat(maskLength) + str.charAt(str.length() - 1);
    }

    /**
     * Genera un hash SHA-256 para datos sensibles en producción
     */
    private String generateHash(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes());
            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return "hash:" + hexString.toString().substring(0, 8) + "...";
        } catch (NoSuchAlgorithmException e) {
            return "hash:unavailable";
        }
    }

    /**
     * Trunca mensajes largos para evitar logs excesivos
     */
    private String truncateMessage(String message) {
        if (message == null) {
            return "null";
        }

        final int MAX_LOG_LENGTH = 100;
        if (message.length() > MAX_LOG_LENGTH) {
            return message.substring(0, MAX_LOG_LENGTH) + "... [truncated]";
        }

        return message;
    }
}

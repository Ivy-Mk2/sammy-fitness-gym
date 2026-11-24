package com.sammyfitness.gym.service.correo;

/** Envío de correos. Implementaciones: SendGrid (con API key) o solo log (sin API key). */
public interface EmailService {

    void enviar(String destinatario, String asunto, String contenidoHtml);
}

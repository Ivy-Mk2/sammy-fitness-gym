package com.sammyfitness.gym.service.correo;

import lombok.extern.slf4j.Slf4j;

/** Se usa cuando no hay SENDGRID_API_KEY: no envía nada, solo registra el correo en el log. */
@Slf4j
public class LogEmailService implements EmailService {

    @Override
    public void enviar(String destinatario, String asunto, String contenidoHtml) {
        log.info("[CORREO SIMULADO] Para: {} | Asunto: {}\n{}", destinatario, asunto, contenidoHtml);
    }
}

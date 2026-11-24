package com.sammyfitness.gym.config;

import com.sammyfitness.gym.service.correo.EmailService;
import com.sammyfitness.gym.service.correo.LogEmailService;
import com.sammyfitness.gym.service.correo.SendGridEmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.time.ZoneId;

@Slf4j
@Configuration
@EnableAsync        // correos en segundo plano: la reserva no depende del envío
@EnableScheduling   // RN-06: proceso diario de membresías
public class AppConfig {

    /** Reloj con la zona horaria del gimnasio: define "hoy" y si una clase ya pasó. */
    @Bean
    public Clock clock(@Value("${app.zona-horaria}") String zonaHoraria) {
        return Clock.system(ZoneId.of(zonaHoraria));
    }

    /** SendGrid si hay API key; si no, una implementación que solo escribe en el log. */
    @Bean
    public EmailService emailService(@Value("${app.mail.sendgrid-api-key:}") String apiKey,
                                     @Value("${app.mail.from}") String remitente,
                                     @Value("${app.mail.from-name}") String nombreRemitente) {
        if (apiKey == null || apiKey.isBlank()) {
            log.info("Correo: SENDGRID_API_KEY no definida, los correos solo se registrarán en el log.");
            return new LogEmailService();
        }
        log.info("Correo: usando SendGrid con remitente {}", remitente);
        return new SendGridEmailService(apiKey, remitente, nombreRemitente);
    }
}

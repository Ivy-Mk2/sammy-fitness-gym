package com.sammyfitness.gym.service.correo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Correos del sistema. Todos los envíos son @Async y capturan sus errores:
 * un fallo de correo nunca afecta a la reserva ni al proceso diario.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacionService {

    private static final DateTimeFormatter FECHA =
            DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-PE"));

    private final EmailService emailService;

    /** Se ejecuta solo si la transacción de la reserva se confirmó (AFTER_COMMIT) y en otro hilo. */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void confirmarReserva(ReservaConfirmadaEvent e) {
        String html = """
                <h2>¡Reserva confirmada!</h2>
                <p>Hola %s, tu lugar está reservado:</p>
                <ul>
                  <li><b>Clase:</b> %s</li>
                  <li><b>Entrenador:</b> %s</li>
                  <li><b>Fecha:</b> %s</li>
                  <li><b>Horario:</b> %s – %s</li>
                </ul>
                <p>Si no puedes asistir, cancela tu reserva desde la web para liberar el cupo.</p>
                <p>Sammy Fitness Gym</p>
                """.formatted(esc(e.nombreCliente()), esc(e.clase()), esc(e.entrenador()),
                FECHA.format(e.fecha()), e.horaInicio(), e.horaFin());
        enviarSeguro(e.correo(), "Reserva confirmada: " + e.clase(), html);
    }

    /** RN-06: aviso de membresía próxima a vencer. */
    @Async
    public void avisarVencimiento(String correo, String nombre, String plan, LocalDate fechaVencimiento, long diasRestantes) {
        String cuando = diasRestantes == 0 ? "hoy" : diasRestantes == 1 ? "mañana" : "en " + diasRestantes + " días";
        String html = """
                <h2>Tu membresía vence %s</h2>
                <p>Hola %s, tu plan <b>%s</b> vence el <b>%s</b>.</p>
                <p>Renueva en recepción para seguir reservando clases sin interrupciones.</p>
                <p>Sammy Fitness Gym</p>
                """.formatted(cuando, esc(nombre), esc(plan), FECHA.format(fechaVencimiento));
        enviarSeguro(correo, "Tu membresía vence " + cuando, html);
    }

    private void enviarSeguro(String correo, String asunto, String html) {
        try {
            emailService.enviar(correo, asunto, html);
        } catch (Exception ex) {
            log.warn("No se pudo enviar el correo '{}' a {}: {}", asunto, correo, ex.getMessage());
        }
    }

    private static String esc(String texto) {
        return texto == null ? "" : HtmlUtils.htmlEscape(texto);
    }
}

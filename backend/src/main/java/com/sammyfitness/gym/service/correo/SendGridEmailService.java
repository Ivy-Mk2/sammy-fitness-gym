package com.sammyfitness.gym.service.correo;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/** Envío real mediante la API v3 de SendGrid. */
@Slf4j
public class SendGridEmailService implements EmailService {

    private final SendGrid sendGrid;
    private final Email remitente;

    public SendGridEmailService(String apiKey, String correoRemitente, String nombreRemitente) {
        this.sendGrid = new SendGrid(apiKey);
        this.remitente = new Email(correoRemitente, nombreRemitente);
    }

    @Override
    public void enviar(String destinatario, String asunto, String contenidoHtml) {
        Mail mail = new Mail(remitente, asunto, new Email(destinatario), new Content("text/html", contenidoHtml));
        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sendGrid.api(request);
            if (response.getStatusCode() >= 400) {
                throw new IllegalStateException("SendGrid respondió " + response.getStatusCode() + ": " + response.getBody());
            }
            log.info("Correo enviado a {} ({})", destinatario, asunto);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo enviar el correo a " + destinatario, e);
        }
    }
}

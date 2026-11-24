package com.sammyfitness.gym.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** RN-06: tarea diaria (por defecto 06:00 hora de Lima). */
@Component
@RequiredArgsConstructor
public class MembresiaScheduler {

    private final ClienteMembresiaService clienteMembresiaService;

    @Scheduled(cron = "${app.membresias.cron}", zone = "${app.zona-horaria}")
    public void ejecutar() {
        clienteMembresiaService.procesarVencimientos();
    }
}

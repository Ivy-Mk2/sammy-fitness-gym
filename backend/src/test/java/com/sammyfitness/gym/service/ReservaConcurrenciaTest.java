package com.sammyfitness.gym.service;

import com.sammyfitness.gym.exception.ConflictoNegocioException;
import com.sammyfitness.gym.model.*;
import com.sammyfitness.gym.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * RN-02 con concurrencia real: dos clientes reservan a la vez el último cupo.
 * Solo una reserva debe confirmarse; la otra falla con conflicto (aforo o @Version → 409).
 * Sin @Transactional: cada hilo confirma su propia transacción. Los datos se limpian al final.
 */
@SpringBootTest
@ActiveProfiles({"test", "dev"})
@Import(DatosPrueba.class)
class ReservaConcurrenciaTest {

    @Autowired DatosPrueba datos;
    @Autowired ReservaService reservaService;
    @Autowired ReservaRepository reservaRepository;
    @Autowired ClaseRepository claseRepository;
    @Autowired MembresiaRepository membresiaRepository;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired PlatformTransactionManager transactionManager;

    private Clase clase;
    private List<Usuario> creados;

    @RepeatedTest(3)
    void soloUnaDeDosReservasSimultaneasOcupaElUltimoCupo() throws Exception {
        Usuario entrenador = datos.usuario(Rol.ENTRENADOR);
        Usuario c1 = datos.clienteConMembresia();
        Usuario c2 = datos.clienteConMembresia();
        creados = List.of(entrenador, c1, c2);
        clase = datos.clase(entrenador, LocalDate.now().plusDays(1), "10:00", "11:00", 1);

        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        Callable<Object> t1 = () -> intentar(salida, c1.getId());
        Callable<Object> t2 = () -> intentar(salida, c2.getId());
        Future<Object> f1 = pool.submit(t1);
        Future<Object> f2 = pool.submit(t2);
        salida.countDown();
        List<Object> resultados = List.of(f1.get(30, TimeUnit.SECONDS), f2.get(30, TimeUnit.SECONDS));
        pool.shutdown();

        long exitos = resultados.stream().filter(r -> r == Boolean.TRUE).count();
        List<Object> errores = resultados.stream().filter(r -> r != Boolean.TRUE).toList();

        assertThat(exitos).isEqualTo(1);
        assertThat(errores).hasSize(1);
        assertThat(errores.get(0)).as("tipo de error: %s", errores.get(0))
                .isInstanceOfAny(ConflictoNegocioException.class, OptimisticLockingFailureException.class);
        assertThat(reservaRepository.countByClaseIdAndEstado(clase.getId(), EstadoReserva.CONFIRMADA)).isEqualTo(1);
    }

    /**
     * Escenario determinista del @Version: la transacción A lee la clase con el bloqueo optimista,
     * mientras tanto B confirma una reserva (sube la versión). Al confirmar, A debe fallar.
     */
    @Test
    void laVersionDeLaClaseDetectaUnaModificacionConcurrente() throws Exception {
        Usuario entrenador = datos.usuario(Rol.ENTRENADOR);
        Usuario c1 = datos.clienteConMembresia();
        Usuario c2 = datos.clienteConMembresia();
        creados = List.of(entrenador, c1, c2);
        clase = datos.clase(entrenador, LocalDate.now().plusDays(1), "10:00", "11:00", 10);
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        assertThatThrownBy(() -> tx.executeWithoutResult(estado -> {
            Clase leidaPorA = claseRepository.findByIdConBloqueo(clase.getId()).orElseThrow();
            // B reserva y confirma en otro hilo mientras A sigue abierta
            ExecutorService otro = Executors.newSingleThreadExecutor();
            try {
                otro.submit(() -> reservaService.reservar(c2.getId(), clase.getId())).get(30, TimeUnit.SECONDS);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            } finally {
                otro.shutdown();
            }
            reservaRepository.save(Reserva.builder().cliente(c1).clase(leidaPorA)
                    .estado(EstadoReserva.CONFIRMADA).build());
        })).isInstanceOf(OptimisticLockingFailureException.class);

        assertThat(reservaRepository.countByClaseIdAndEstado(clase.getId(), EstadoReserva.CONFIRMADA)).isEqualTo(1);
    }

    private Object intentar(CountDownLatch salida, Long clienteId) throws InterruptedException {
        salida.await();
        try {
            reservaService.reservar(clienteId, clase.getId());
            return Boolean.TRUE;
        } catch (Exception e) {
            return e;
        }
    }

    @AfterEach
    void limpiar() {
        if (clase != null) {
            reservaRepository.deleteAll(reservaRepository.findByClaseIdAndEstadoIn(clase.getId(), List.of(EstadoReserva.values())));
            claseRepository.deleteById(clase.getId());
        }
        if (creados != null) {
            for (Usuario u : creados) {
                membresiaRepository.deleteAll(membresiaRepository.findByUsuarioIdOrderByFechaInicioDesc(u.getId()));
                usuarioRepository.deleteById(u.getId());
            }
        }
    }
}

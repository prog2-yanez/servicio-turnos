package prog2.servicioturnos.reservas.infrastructure.recuperacion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import prog2.servicioturnos.reservas.domain.ports.in.RecuperarProcesosUseCase;

// Corre la recuperación apenas arranca el servicio (así se retoma lo que quedó a medias antes de un reinicio) y
// después cada turnos.recuperacion.intervalo, contado desde que termina la ronda anterior: nunca se superponen.
@Component
@ConditionalOnProperty(name = "turnos.recuperacion.habilitada", havingValue = "true", matchIfMissing = true)
public class RecuperacionProgramada {

    private static final Logger log = LoggerFactory.getLogger(RecuperacionProgramada.class);

    private final RecuperarProcesosUseCase recuperarProcesos;

    public RecuperacionProgramada(RecuperarProcesosUseCase recuperarProcesos) {
        this.recuperarProcesos = recuperarProcesos;
    }

    // Si una ronda falla (por ejemplo, sin base), queda en el log y se reintenta en la siguiente.
    @Scheduled(initialDelay = 0, fixedDelayString = "${turnos.recuperacion.intervalo}")
    public void ejecutar() {
        try {
            recuperarProcesos.recuperar();
        } catch (RuntimeException e) {
            log.warn("Recuperación: la ronda falló y se reintenta en la próxima: {}", e.getMessage());
        }
    }
}

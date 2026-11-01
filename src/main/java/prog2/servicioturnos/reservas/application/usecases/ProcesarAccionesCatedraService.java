package prog2.servicioturnos.reservas.application.usecases;

import java.time.Clock;
import java.util.Optional;
import java.util.function.UnaryOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import prog2.servicioturnos.reservas.domain.model.EventoCatedra;
import prog2.servicioturnos.reservas.domain.model.ProcesoReserva;
import prog2.servicioturnos.reservas.domain.ports.in.ProcesarAccionesCatedraUseCase;
import prog2.servicioturnos.reservas.domain.ports.out.ProcesoReservaRepository;

// Aplica los eventos de la cátedra al proceso local. Un evento que no corresponde (proceso desconocido, ya terminado
// o en otro paso) se deja en el log y no se aplica: un evento tardío nunca reabre un proceso cerrado.
@Service
public class ProcesarAccionesCatedraService implements ProcesarAccionesCatedraUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcesarAccionesCatedraService.class);

    private final ProcesoReservaRepository procesos;
    private final Clock reloj;

    public ProcesarAccionesCatedraService(ProcesoReservaRepository procesos, Clock reloj) {
        this.procesos = procesos;
        this.reloj = reloj;
    }

    @Override
    public void telefonoPedido(EventoCatedra evento) {
        aplicar(evento, "pedido de teléfono",
                proceso -> proceso.telefonoPedido(evento.eventId(), evento.venceEn(), reloj.instant()));
    }

    @Override
    public void telefonoRechazado(EventoCatedra evento) {
        aplicar(evento, "rechazo del teléfono",
                proceso -> proceso.telefonoRechazado(evento.venceEn(), reloj.instant()));
    }

    private void aplicar(EventoCatedra evento, String accion, UnaryOperator<ProcesoReserva> cambio) {
        Optional<ProcesoReserva> encontrado = procesos.buscarPorReservationProcessId(evento.reservationProcessId());
        if (encontrado.isEmpty()) {
            log.warn("Se ignora el {} del evento {}: no hay un proceso con ese reservationProcessId",
                    accion, evento.eventId());
            return;
        }
        ProcesoReserva proceso = encontrado.get();
        try {
            procesos.guardar(cambio.apply(proceso));
            log.info("Proceso {}: se aplicó el {} del evento {}", proceso.id(), accion, evento.eventId());
        } catch (IllegalStateException e) {
            log.warn("Se ignora el {} del evento {} para el proceso {}: {}",
                    accion, evento.eventId(), proceso.id(), e.getMessage());
        }
    }
}

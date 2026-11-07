package prog2.servicioturnos.reservas.application.usecases;

import java.time.Clock;
import java.util.Optional;
import java.util.function.UnaryOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import prog2.servicioturnos.reservas.domain.model.EstadoProceso;
import prog2.servicioturnos.reservas.domain.model.EventoCatedra;
import prog2.servicioturnos.reservas.domain.model.ProcesoReserva;
import prog2.servicioturnos.reservas.domain.ports.in.ProcesarAccionesCatedraUseCase;
import prog2.servicioturnos.reservas.domain.ports.out.EventosProcesados;
import prog2.servicioturnos.reservas.domain.ports.out.ProcesoReservaRepository;

// CU-06. Cada evento se aplica en una transacción que también registra su eventId: si falla, no queda ninguno de
// los dos y Kafka lo reintenta; si ya estaba registrado, es un duplicado y no se aplica. Un evento que no
// corresponde (proceso desconocido, ya terminado o en otro paso) se deja en el log: un evento tardío no reabre nada.
@Service
public class ProcesarAccionesCatedraService implements ProcesarAccionesCatedraUseCase {

    static final int LARGO_MAXIMO_MOTIVO = 500;

    private static final Logger log = LoggerFactory.getLogger(ProcesarAccionesCatedraService.class);

    private final ProcesoReservaRepository procesos;
    private final EventosProcesados eventosProcesados;
    private final Clock reloj;

    public ProcesarAccionesCatedraService(ProcesoReservaRepository procesos, EventosProcesados eventosProcesados,
            Clock reloj) {
        this.procesos = procesos;
        this.eventosProcesados = eventosProcesados;
        this.reloj = reloj;
    }

    @Override
    @Transactional
    public void telefonoPedido(EventoCatedra evento) {
        aplicar(evento, proceso -> proceso.telefonoPedido(evento.eventId(), evento.venceEn(), reloj.instant()));
    }

    @Override
    @Transactional
    public void telefonoRechazado(EventoCatedra evento) {
        aplicar(evento, proceso -> proceso.telefonoRechazado(evento.venceEn(), reloj.instant()));
    }

    @Override
    @Transactional
    public void confirmado(EventoCatedra evento) {
        aplicar(evento, proceso -> {
            if (proceso.estado() == EstadoProceso.VENCIDO) {
                log.warn("Proceso {}: la cátedra confirmó una reserva que turnos había dado por vencida",
                        proceso.id());
            }
            return proceso.confirmado(evento.reservationId(), reloj.instant());
        });
    }

    @Override
    @Transactional
    public void vencido(EventoCatedra evento) {
        aplicar(evento, proceso -> proceso.vencido(reloj.instant()));
    }

    @Override
    @Transactional
    public void invalido(EventoCatedra evento) {
        aplicar(evento, proceso -> proceso.invalido(recortar(evento.motivo()), reloj.instant()));
    }

    @Override
    @Transactional
    public void cancelado(EventoCatedra evento) {
        aplicar(evento, proceso -> proceso.cancelado(evento.reservationId(), recortar(evento.motivo()),
                reloj.instant()));
    }

    private void aplicar(EventoCatedra evento, UnaryOperator<ProcesoReserva> cambio) {
        if (!eventosProcesados.registrar(evento.eventId(), evento.tipo())) {
            log.info("Evento {} repetido: se descarta", evento.eventId());
            return;
        }
        Optional<ProcesoReserva> encontrado = procesos.buscarPorReservationProcessId(evento.reservationProcessId());
        if (encontrado.isEmpty()) {
            log.warn("Se ignora el evento {} ({}): no hay un proceso con ese reservationProcessId",
                    evento.eventId(), evento.tipo());
            return;
        }
        ProcesoReserva proceso = encontrado.get();
        try {
            ProcesoReserva actualizado = procesos.guardar(cambio.apply(proceso));
            log.info("Proceso {}: {} -> {} por el evento {} ({})", proceso.id(), proceso.estado(),
                    actualizado.estado(), evento.eventId(), evento.tipo());
        } catch (IllegalStateException e) {
            log.warn("Se ignora el evento {} ({}) para el proceso {}: {}",
                    evento.eventId(), evento.tipo(), proceso.id(), e.getMessage());
        }
    }

    private static String recortar(String motivo) {
        return motivo == null || motivo.length() <= LARGO_MAXIMO_MOTIVO
                ? motivo : motivo.substring(0, LARGO_MAXIMO_MOTIVO);
    }
}

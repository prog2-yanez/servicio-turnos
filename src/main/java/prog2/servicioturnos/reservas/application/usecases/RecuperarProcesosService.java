package prog2.servicioturnos.reservas.application.usecases;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import prog2.servicioturnos.config.RecuperacionProperties;
import prog2.servicioturnos.disponibilidad.domain.exception.CatedraNoDisponibleException;
import prog2.servicioturnos.reservas.domain.model.ProcesoReserva;
import prog2.servicioturnos.reservas.domain.model.ReservaCatedra;
import prog2.servicioturnos.reservas.domain.model.ResultadoRecuperacion;
import prog2.servicioturnos.reservas.domain.ports.in.RecuperarProcesosUseCase;
import prog2.servicioturnos.reservas.domain.ports.out.ProcesoReservaRepository;
import prog2.servicioturnos.reservas.domain.ports.out.ReservasCatedra;

// CU-10. Reconcilia los procesos abiertos con la cátedra y cierra los que vencieron sin resultado. Solo consulta:
// nunca vuelve a crear ni a confirmar un hold, así no se puede duplicar una reserva.
@Service
public class RecuperarProcesosService implements RecuperarProcesosUseCase {

    static final String MOTIVO_FALLIDA = "La cátedra informó la reserva como fallida";

    private static final Logger log = LoggerFactory.getLogger(RecuperarProcesosService.class);

    private final ProcesoReservaRepository procesos;
    private final ReservasCatedra reservasCatedra;
    private final RecuperacionProperties propiedades;
    private final Clock reloj;

    public RecuperarProcesosService(ProcesoReservaRepository procesos, ReservasCatedra reservasCatedra,
            RecuperacionProperties propiedades, Clock reloj) {
        this.procesos = procesos;
        this.reservasCatedra = reservasCatedra;
        this.propiedades = propiedades;
        this.reloj = reloj;
    }

    @Override
    public ResultadoRecuperacion recuperar() {
        List<ProcesoReserva> abiertos = procesos.listarAbiertos();
        int resueltos = 0;
        Map<Long, List<ProcesoReserva>> porUsuario =
                abiertos.stream().collect(Collectors.groupingBy(ProcesoReserva::usuarioId));
        for (Map.Entry<Long, List<ProcesoReserva>> usuario : porUsuario.entrySet()) {
            Map<String, ReservaCatedra> deLaCatedra = consultarCatedra(usuario.getKey(), usuario.getValue());
            for (ProcesoReserva proceso : usuario.getValue()) {
                if (recuperar(proceso, deLaCatedra)) {
                    resueltos++;
                }
            }
        }
        ResultadoRecuperacion resultado =
                new ResultadoRecuperacion(abiertos.size(), resueltos, abiertos.size() - resueltos);
        if (resultado.revisados() > 0) {
            log.info("Recuperación: {} procesos abiertos revisados, {} resueltos, {} siguen pendientes",
                    resultado.revisados(), resultado.resueltos(), resultado.pendientes());
        }
        return resultado;
    }

    // Una sola consulta por usuario. Si la cátedra no responde, se sigue sin sus datos: se reintenta en la próxima
    // ronda y, mientras tanto, los vencidos se cierran igual.
    private Map<String, ReservaCatedra> consultarCatedra(long usuarioId, List<ProcesoReserva> deUnUsuario) {
        if (deUnUsuario.stream().allMatch(proceso -> proceso.reservationProcessId() == null)) {
            return Map.of();
        }
        try {
            return reservasCatedra.deUnPaciente(String.valueOf(usuarioId)).stream()
                    .filter(reserva -> reserva.reservationProcessId() != null)
                    .collect(Collectors.toMap(ReservaCatedra::reservationProcessId, Function.identity(),
                            (primera, segunda) -> primera));
        } catch (CatedraNoDisponibleException e) {
            log.warn("Recuperación: la cátedra no respondió las reservas del usuario {}; se reintenta en la "
                    + "próxima ronda", usuarioId);
            return Map.of();
        }
    }

    // Devuelve true si el proceso quedó con un estado final.
    private boolean recuperar(ProcesoReserva proceso, Map<String, ReservaCatedra> deLaCatedra) {
        try {
            Instant ahora = reloj.instant();
            Optional<ProcesoReserva> cambio = Optional.ofNullable(proceso.reservationProcessId())
                    .map(deLaCatedra::get)
                    .flatMap(reserva -> segunLaCatedra(proceso, reserva, ahora))
                    .or(() -> venceSinResultado(proceso, ahora));
            if (cambio.isEmpty()) {
                return false;
            }
            return guardarSiNadieLoCambio(proceso, cambio.get());
        } catch (RuntimeException e) {
            log.warn("Recuperación: no se pudo revisar el proceso {}: {}", proceso.id(), e.getMessage());
            return false;
        }
    }

    // Solo un estado final de la cátedra cambia el proceso; PHONE_PENDING lo resuelve el evento de Kafka.
    private static Optional<ProcesoReserva> segunLaCatedra(ProcesoReserva proceso, ReservaCatedra reserva,
            Instant ahora) {
        return Optional.ofNullable(switch (reserva.estado()) {
            case CONFIRMADA -> proceso.confirmado(reserva.reservationId(), ahora);
            case CANCELADA -> proceso.cancelado(reserva.reservationId(), reserva.motivoCancelacion(), ahora);
            case FALLIDA -> proceso.invalido(MOTIVO_FALLIDA, ahora);
            case TELEFONO_PENDIENTE -> null;
        });
    }

    // Sin vencimiento conocido se cuenta desde que se creó el proceso.
    private Optional<ProcesoReserva> venceSinResultado(ProcesoReserva proceso, Instant ahora) {
        Instant limite = proceso.venceEn() != null
                ? proceso.venceEn().plus(propiedades.margen())
                : proceso.creadoEn().plus(propiedades.esperaSinHold());
        return limite.isAfter(ahora) ? Optional.empty() : Optional.of(proceso.vencido(ahora));
    }

    // Un evento de la cátedra pudo haber cambiado el proceso mientras se revisaba: en ese caso manda el evento.
    private boolean guardarSiNadieLoCambio(ProcesoReserva revisado, ProcesoReserva cambio) {
        Optional<ProcesoReserva> actual = procesos.buscarPorId(revisado.id());
        if (actual.isEmpty() || !actual.get().actualizadoEn().equals(revisado.actualizadoEn())) {
            log.info("Recuperación: el proceso {} cambió mientras se revisaba; se deja como está", revisado.id());
            return actual.map(proceso -> !proceso.estaAbierto()).orElse(false);
        }
        procesos.guardar(cambio);
        log.info("Recuperación: proceso {} {} -> {}", revisado.id(), revisado.estado(), cambio.estado());
        return true;
    }
}

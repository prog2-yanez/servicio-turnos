package prog2.servicioturnos.reservas.application.usecases;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import prog2.servicioturnos.disponibilidad.domain.exception.CatedraNoDisponibleException;
import prog2.servicioturnos.reservas.domain.model.EstadoProceso;
import prog2.servicioturnos.reservas.domain.model.EstadoReservaCatedra;
import prog2.servicioturnos.reservas.domain.model.Paciente;
import prog2.servicioturnos.reservas.domain.model.ProcesoReserva;
import prog2.servicioturnos.reservas.domain.model.ReservaCatedra;
import prog2.servicioturnos.reservas.domain.model.ReservaPropia;
import prog2.servicioturnos.reservas.domain.model.ReservasPropias;
import prog2.servicioturnos.reservas.domain.ports.in.ListarReservasPropiasUseCase;
import prog2.servicioturnos.reservas.domain.ports.out.CuentasPacientes;
import prog2.servicioturnos.reservas.domain.ports.out.ProcesoReservaRepository;
import prog2.servicioturnos.reservas.domain.ports.out.ReservasCatedra;

// CU-08. Combina los procesos de turnos con las reservas de la cátedra. Solo se listan reservas de verdad: las que
// la cátedra registró (pendientes del teléfono, confirmadas o canceladas). Si local y cátedra no coinciden, se
// muestra lo de la cátedra sin tocar la base: un GET no cambia datos; eso lo corrige la recuperación.
@Service
public class ListarReservasPropiasService implements ListarReservasPropiasUseCase {

    // Estados locales que corresponden a una reserva que la cátedra ya registró.
    private static final Set<EstadoProceso> ESTADOS_DE_RESERVA = Set.of(EstadoProceso.ESPERANDO_TELEFONO,
            EstadoProceso.TELEFONO_ENVIADO, EstadoProceso.TELEFONO_RECHAZADO, EstadoProceso.CONFIRMADO,
            EstadoProceso.CANCELADO);

    private static final Comparator<ReservaPropia> MAS_NUEVAS_PRIMERO =
            Comparator.comparing(ReservaPropia::fecha).thenComparing(ReservaPropia::horaInicio).reversed();

    private static final Logger log = LoggerFactory.getLogger(ListarReservasPropiasService.class);

    private final CuentasPacientes cuentas;
    private final ProcesoReservaRepository procesos;
    private final ReservasCatedra reservasCatedra;

    public ListarReservasPropiasService(CuentasPacientes cuentas, ProcesoReservaRepository procesos,
            ReservasCatedra reservasCatedra) {
        this.cuentas = cuentas;
        this.procesos = procesos;
        this.reservasCatedra = reservasCatedra;
    }

    @Override
    public ReservasPropias listar(String login) {
        Paciente paciente = cuentas.obtener(login);
        List<ProcesoReserva> locales = procesos.listarDe(paciente.usuarioId()).stream()
                .filter(proceso -> ESTADOS_DE_RESERVA.contains(proceso.estado()))
                .toList();
        List<ReservaCatedra> deLaCatedra;
        try {
            deLaCatedra = reservasCatedra.deUnPaciente(paciente.externalPatientId()).stream()
                    .filter(reserva -> reserva.estado() != EstadoReservaCatedra.FALLIDA)
                    .toList();
        } catch (CatedraNoDisponibleException e) {
            log.warn("Se listan las reservas del usuario {} sin la cátedra: pueden estar desactualizadas",
                    paciente.usuarioId());
            return new ReservasPropias(ordenar(locales.stream().map(ListarReservasPropiasService::local).toList()),
                    true);
        }
        return new ReservasPropias(ordenar(combinar(locales, deLaCatedra)), false);
    }

    private static List<ReservaPropia> combinar(List<ProcesoReserva> locales, List<ReservaCatedra> deLaCatedra) {
        Map<String, ProcesoReserva> porReservationProcessId = locales.stream()
                .filter(proceso -> proceso.reservationProcessId() != null)
                .collect(Collectors.toMap(ProcesoReserva::reservationProcessId, Function.identity(),
                        (uno, otro) -> uno));
        List<ReservaPropia> reservas = new ArrayList<>();
        for (ReservaCatedra reserva : deLaCatedra) {
            reservas.add(combinada(reserva, porReservationProcessId.remove(reserva.reservationProcessId())));
        }
        // Las que la cátedra no devolvió se muestran con lo que sabe turnos.
        porReservationProcessId.values().forEach(proceso -> reservas.add(local(proceso)));
        locales.stream().filter(proceso -> proceso.reservationProcessId() == null)
                .forEach(proceso -> reservas.add(local(proceso)));
        return reservas;
    }

    private static ReservaPropia combinada(ReservaCatedra reserva, ProcesoReserva proceso) {
        return new ReservaPropia(proceso == null ? null : proceso.id(), estado(reserva, proceso), reserva.fecha(),
                reserva.horaInicio(), reserva.horaFin(), reserva.profesionalId(), reserva.profesionalNombre(),
                reserva.profesionalApellido(), reserva.confirmadaEn(), reserva.canceladaEn(),
                reserva.motivoCancelacion());
    }

    private static ReservaPropia local(ProcesoReserva proceso) {
        String motivo = proceso.estado() == EstadoProceso.CANCELADO ? proceso.motivo() : null;
        return new ReservaPropia(proceso.id(), proceso.estado(), proceso.fecha(), proceso.horaInicio(), null,
                proceso.profesionalId(), null, null, null, null, motivo);
    }

    // Manda la cátedra. Mientras espera el teléfono se usa el paso local, que es más preciso (enviado, rechazado).
    private static EstadoProceso estado(ReservaCatedra reserva, ProcesoReserva proceso) {
        return switch (reserva.estado()) {
            case CONFIRMADA -> EstadoProceso.CONFIRMADO;
            case CANCELADA -> EstadoProceso.CANCELADO;
            case TELEFONO_PENDIENTE, FALLIDA -> proceso != null && proceso.estaAbierto()
                    ? proceso.estado() : EstadoProceso.ESPERANDO_TELEFONO;
        };
    }

    private static List<ReservaPropia> ordenar(List<ReservaPropia> reservas) {
        return reservas.stream().sorted(MAS_NUEVAS_PRIMERO).toList();
    }
}

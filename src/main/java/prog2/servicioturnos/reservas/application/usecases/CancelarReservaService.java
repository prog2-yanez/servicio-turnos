package prog2.servicioturnos.reservas.application.usecases;

import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import prog2.servicioturnos.disponibilidad.domain.exception.CatedraNoDisponibleException;
import prog2.servicioturnos.reservas.domain.exception.CancelacionNoConfirmadaException;
import prog2.servicioturnos.reservas.domain.exception.ProcesoNoEncontradoException;
import prog2.servicioturnos.reservas.domain.exception.ReservaNoCancelableException;
import prog2.servicioturnos.reservas.domain.exception.ResultadoInciertoException;
import prog2.servicioturnos.reservas.domain.model.EstadoProceso;
import prog2.servicioturnos.reservas.domain.model.EstadoReservaCatedra;
import prog2.servicioturnos.reservas.domain.model.Paciente;
import prog2.servicioturnos.reservas.domain.model.ProcesoReserva;
import prog2.servicioturnos.reservas.domain.model.ReservaCatedra;
import prog2.servicioturnos.reservas.domain.ports.in.CancelarReservaUseCase;
import prog2.servicioturnos.reservas.domain.ports.out.CuentasPacientes;
import prog2.servicioturnos.reservas.domain.ports.out.ProcesoReservaRepository;
import prog2.servicioturnos.reservas.domain.ports.out.ReservasCatedra;

// CU-09. La reserva queda cancelada por la respuesta de la cátedra o por el evento AppointmentCancelled, lo que
// llegue primero. Nunca se marca cancelada sin que la cátedra lo confirme.
@Service
public class CancelarReservaService implements CancelarReservaUseCase {

    private static final Logger log = LoggerFactory.getLogger(CancelarReservaService.class);

    private final CuentasPacientes cuentas;
    private final ProcesoReservaRepository procesos;
    private final ReservasCatedra reservasCatedra;
    private final Clock reloj;

    public CancelarReservaService(CuentasPacientes cuentas, ProcesoReservaRepository procesos,
            ReservasCatedra reservasCatedra, Clock reloj) {
        this.cuentas = cuentas;
        this.procesos = procesos;
        this.reservasCatedra = reservasCatedra;
        this.reloj = reloj;
    }

    @Override
    public ProcesoReserva cancelar(String login, long procesoId, String motivo) {
        Paciente paciente = cuentas.obtener(login);
        ProcesoReserva proceso = procesos.buscarPorId(procesoId)
                .filter(encontrado -> encontrado.esDe(paciente.usuarioId()))
                .orElseThrow(() -> new ProcesoNoEncontradoException(procesoId));
        // Cancelar dos veces no es un error: se devuelve como está, sin volver a llamar a la cátedra.
        if (proceso.estado() == EstadoProceso.CANCELADO) {
            return proceso;
        }
        // Un turno retenido todavía sin confirmar se deja vencer.
        if (proceso.estado() != EstadoProceso.CONFIRMADO) {
            throw new ReservaNoCancelableException();
        }
        String reservationId;
        try {
            reservationId = reservasCatedra.cancelar(proceso.reservationProcessId(), motivo);
        } catch (ResultadoInciertoException e) {
            reservationId = verificarCancelacion(proceso, paciente);
        }
        return guardarCancelado(procesoId, reservationId, motivo);
    }

    // Sin respuesta no se sabe si la cátedra canceló: se consulta la reserva antes de informar algo.
    private String verificarCancelacion(ProcesoReserva proceso, Paciente paciente) {
        try {
            return reservasCatedra.deUnPaciente(paciente.externalPatientId()).stream()
                    .filter(reserva -> proceso.reservationProcessId().equals(reserva.reservationProcessId()))
                    .filter(reserva -> reserva.estado() == EstadoReservaCatedra.CANCELADA)
                    .map(ReservaCatedra::reservationId)
                    .findFirst()
                    .orElseThrow(CancelacionNoConfirmadaException::new);
        } catch (CatedraNoDisponibleException e) {
            throw new CancelacionNoConfirmadaException();
        }
    }

    // Se relee porque el evento AppointmentCancelled pudo haber llegado mientras se esperaba a la cátedra.
    private ProcesoReserva guardarCancelado(long procesoId, String reservationId, String motivo) {
        ProcesoReserva actual = procesos.buscarPorId(procesoId).orElseThrow(
                () -> new ProcesoNoEncontradoException(procesoId));
        if (actual.estado() == EstadoProceso.CANCELADO) {
            return actual;
        }
        ProcesoReserva cancelado = procesos.guardar(actual.cancelado(reservationId, motivo, reloj.instant()));
        log.info("Proceso {}: cancelado por el usuario", procesoId);
        return cancelado;
    }
}

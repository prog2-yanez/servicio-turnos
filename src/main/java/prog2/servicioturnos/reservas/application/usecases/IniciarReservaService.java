package prog2.servicioturnos.reservas.application.usecases;

import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import prog2.servicioturnos.disponibilidad.domain.exception.CatedraNoDisponibleException;
import prog2.servicioturnos.disponibilidad.domain.model.TurnosLibresDelProfesional;
import prog2.servicioturnos.disponibilidad.domain.ports.in.ConsultarTurnosLibresUseCase;
import prog2.servicioturnos.reservas.domain.exception.DatosDeCuentaIncompletosException;
import prog2.servicioturnos.reservas.domain.exception.ProcesoAbiertoException;
import prog2.servicioturnos.reservas.domain.exception.ProcesoVencidoException;
import prog2.servicioturnos.reservas.domain.exception.ReservaRechazadaException;
import prog2.servicioturnos.reservas.domain.exception.ResultadoInciertoException;
import prog2.servicioturnos.reservas.domain.exception.TurnoNoDisponibleException;
import prog2.servicioturnos.reservas.domain.exception.TurnoOcupadoException;
import prog2.servicioturnos.reservas.domain.model.HoldCreado;
import prog2.servicioturnos.reservas.domain.model.Paciente;
import prog2.servicioturnos.reservas.domain.model.PedidoReserva;
import prog2.servicioturnos.reservas.domain.model.ProcesoReserva;
import prog2.servicioturnos.reservas.domain.ports.in.IniciarReservaUseCase;
import prog2.servicioturnos.reservas.domain.ports.out.CuentasPacientes;
import prog2.servicioturnos.reservas.domain.ports.out.HoldsCatedra;
import prog2.servicioturnos.reservas.domain.ports.out.ProcesoReservaRepository;

// CU-04. Sin transacción propia: cada paso se guarda apenas ocurre, así el proceso refleja lo que ya se le pidió a
// la cátedra aunque el paso siguiente falle.
@Service
public class IniciarReservaService implements IniciarReservaUseCase {

    private static final Logger log = LoggerFactory.getLogger(IniciarReservaService.class);

    private final CuentasPacientes cuentas;
    private final ProcesoReservaRepository procesos;
    private final ConsultarTurnosLibresUseCase consultarTurnosLibres;
    private final HoldsCatedra holds;
    private final Clock reloj;

    public IniciarReservaService(CuentasPacientes cuentas, ProcesoReservaRepository procesos,
            ConsultarTurnosLibresUseCase consultarTurnosLibres, HoldsCatedra holds, Clock reloj) {
        this.cuentas = cuentas;
        this.procesos = procesos;
        this.consultarTurnosLibres = consultarTurnosLibres;
        this.holds = holds;
        this.reloj = reloj;
    }

    @Override
    public ProcesoReserva iniciar(String login, PedidoReserva pedido) {
        Paciente paciente = cuentas.obtener(login);
        if (!paciente.tieneNombreCompleto()) {
            throw new DatosDeCuentaIncompletosException();
        }
        if (procesos.tieneProcesoAbierto(paciente.usuarioId())) {
            throw new ProcesoAbiertoException();
        }
        long versionCatalogo = validarQueSigueLibre(pedido);
        // Se registra antes de llamar a la cátedra: si algo falla, queda constancia de qué se intentó.
        ProcesoReserva proceso = procesos.guardar(ProcesoReserva.iniciar(paciente.usuarioId(), pedido.profesionalId(),
                pedido.fecha(), pedido.horaInicio(), versionCatalogo, ahora()));
        try {
            HoldCreado hold = holds.crearHold(pedido.profesionalId(), pedido.fecha(), pedido.horaInicio(),
                    paciente.externalPatientId());
            proceso = procesos.guardar(
                    proceso.conHold(hold.holdId(), hold.reservationProcessId(), hold.venceEn(), ahora()));
            Instant venceEn = holds.confirmar(hold.holdId(), paciente.externalPatientId(),
                    paciente.nombre().strip(), paciente.apellido().strip());
            return procesos.guardar(proceso.esperandoTelefono(venceEn, ahora()));
        } catch (ResultadoInciertoException e) {
            log.warn("El proceso {} queda INCIERTO: {}", proceso.id(), e.getMessage());
            return procesos.guardar(proceso.incierto(ahora()));
        } catch (ProcesoVencidoException e) {
            procesos.guardar(proceso.vencido(ahora()));
            throw e;
        } catch (TurnoOcupadoException | TurnoNoDisponibleException | ReservaRechazadaException
                 | CatedraNoDisponibleException e) {
            procesos.guardar(proceso.rechazado(ahora()));
            throw e;
        }
    }

    // Vuelve a validar con el catálogo y la cátedra que el turno siga vigente y libre (agenda, ocupaciones, hora).
    private long validarQueSigueLibre(PedidoReserva pedido) {
        TurnosLibresDelProfesional libres = consultarTurnosLibres.consultar(pedido.profesionalId(), pedido.fecha());
        boolean sigueLibre = libres.turnos().stream()
                .anyMatch(turno -> turno.horaInicio().equals(pedido.horaInicio()));
        if (!sigueLibre) {
            throw new TurnoNoDisponibleException();
        }
        return libres.versionCatalogo();
    }

    private Instant ahora() {
        return reloj.instant();
    }
}

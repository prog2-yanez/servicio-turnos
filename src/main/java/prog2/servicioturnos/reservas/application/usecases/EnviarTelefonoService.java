package prog2.servicioturnos.reservas.application.usecases;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import prog2.servicioturnos.reservas.domain.exception.ProcesoNoEncontradoException;
import prog2.servicioturnos.reservas.domain.exception.ProcesoVencidoException;
import prog2.servicioturnos.reservas.domain.exception.TelefonoNoPedidoException;
import prog2.servicioturnos.reservas.domain.model.ProcesoReserva;
import prog2.servicioturnos.reservas.domain.model.Telefono;
import prog2.servicioturnos.reservas.domain.model.TelefonoInformado;
import prog2.servicioturnos.reservas.domain.ports.in.EnviarTelefonoUseCase;
import prog2.servicioturnos.reservas.domain.ports.out.CuentasPacientes;
import prog2.servicioturnos.reservas.domain.ports.out.ProcesoReservaRepository;
import prog2.servicioturnos.reservas.domain.ports.out.PublicadorTelefono;

// CU-05. El teléfono no se guarda en turnos: es un dato personal que solo necesita la cátedra.
@Service
public class EnviarTelefonoService implements EnviarTelefonoUseCase {

    private final CuentasPacientes cuentas;
    private final ProcesoReservaRepository procesos;
    private final PublicadorTelefono publicador;
    private final Clock reloj;

    public EnviarTelefonoService(CuentasPacientes cuentas, ProcesoReservaRepository procesos,
            PublicadorTelefono publicador, Clock reloj) {
        this.cuentas = cuentas;
        this.procesos = procesos;
        this.publicador = publicador;
        this.reloj = reloj;
    }

    @Override
    public ProcesoReserva enviar(String login, long procesoId, String telefono) {
        long usuarioId = cuentas.obtener(login).usuarioId();
        ProcesoReserva proceso = procesos.buscarPorId(procesoId)
                .filter(encontrado -> encontrado.usuarioId() == usuarioId)
                .orElseThrow(() -> new ProcesoNoEncontradoException(procesoId));
        if (!proceso.puedeEnviarTelefono()) {
            throw new TelefonoNoPedidoException();
        }
        Instant ahora = reloj.instant();
        if (proceso.vencioAntesDe(ahora)) {
            procesos.guardar(proceso.vencido(ahora));
            throw new ProcesoVencidoException();
        }
        Telefono normalizado = Telefono.normalizar(telefono);
        // Un eventId nuevo en cada envío: si el teléfono se rechaza y se corrige, es otro evento.
        publicador.publicar(new TelefonoInformado(UUID.randomUUID().toString(), proceso.reservationProcessId(),
                proceso.holdId(), proceso.requestEventId(), normalizado, ahora));
        return procesos.guardar(proceso.telefonoEnviado(ahora));
    }
}

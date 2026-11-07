package prog2.servicioturnos.reservas.application.usecases;

import org.springframework.stereotype.Service;
import prog2.servicioturnos.reservas.domain.exception.ProcesoNoEncontradoException;
import prog2.servicioturnos.reservas.domain.exception.SinProcesoEnCursoException;
import prog2.servicioturnos.reservas.domain.model.ProcesoReserva;
import prog2.servicioturnos.reservas.domain.ports.in.ConsultarProcesoUseCase;
import prog2.servicioturnos.reservas.domain.ports.out.CuentasPacientes;
import prog2.servicioturnos.reservas.domain.ports.out.ProcesoReservaRepository;

// CU-07. El usuario sale del token: nadie puede ver un proceso ajeno cambiando el id.
@Service
public class ConsultarProcesoService implements ConsultarProcesoUseCase {

    private final CuentasPacientes cuentas;
    private final ProcesoReservaRepository procesos;

    public ConsultarProcesoService(CuentasPacientes cuentas, ProcesoReservaRepository procesos) {
        this.cuentas = cuentas;
        this.procesos = procesos;
    }

    @Override
    public ProcesoReserva consultar(String login, long procesoId) {
        long usuarioId = cuentas.obtener(login).usuarioId();
        return procesos.buscarPorId(procesoId)
                .filter(proceso -> proceso.esDe(usuarioId))
                .orElseThrow(() -> new ProcesoNoEncontradoException(procesoId));
    }

    @Override
    public ProcesoReserva enCurso(String login) {
        long usuarioId = cuentas.obtener(login).usuarioId();
        return procesos.buscarAbiertoDe(usuarioId).orElseThrow(SinProcesoEnCursoException::new);
    }
}

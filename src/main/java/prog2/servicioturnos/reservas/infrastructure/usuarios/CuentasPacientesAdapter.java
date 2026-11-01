package prog2.servicioturnos.reservas.infrastructure.usuarios;

import org.springframework.stereotype.Component;
import prog2.servicioturnos.reservas.domain.model.Paciente;
import prog2.servicioturnos.reservas.domain.ports.out.CuentasPacientes;
import prog2.servicioturnos.usuarios.domain.model.Usuario;
import prog2.servicioturnos.usuarios.domain.ports.in.ConsultarCuentaUseCase;

// Reservas usa el caso de uso público de usuarios, no su base: cada módulo es dueño de sus datos.
@Component
public class CuentasPacientesAdapter implements CuentasPacientes {

    private final ConsultarCuentaUseCase consultarCuenta;

    public CuentasPacientesAdapter(ConsultarCuentaUseCase consultarCuenta) {
        this.consultarCuenta = consultarCuenta;
    }

    @Override
    public Paciente obtener(String login) {
        Usuario usuario = consultarCuenta.consultar(login);
        return new Paciente(usuario.id(), usuario.nombre(), usuario.apellido());
    }
}

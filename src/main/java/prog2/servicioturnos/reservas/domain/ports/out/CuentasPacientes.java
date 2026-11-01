package prog2.servicioturnos.reservas.domain.ports.out;

import prog2.servicioturnos.reservas.domain.model.Paciente;

// Los datos de la cuenta del usuario, sin que reservas dependa de cómo se guardan los usuarios.
public interface CuentasPacientes {

    Paciente obtener(String login);
}

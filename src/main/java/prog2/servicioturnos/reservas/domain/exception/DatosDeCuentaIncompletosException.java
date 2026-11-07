package prog2.servicioturnos.reservas.domain.exception;

import prog2.servicioturnos.shared.exception.PedidoInvalidoException;

// Al registrarse el nombre y el apellido son opcionales, pero la cátedra los exige para reservar.
public class DatosDeCuentaIncompletosException extends PedidoInvalidoException {

    public DatosDeCuentaIncompletosException() {
        super("datos-de-cuenta-incompletos",
                "Para reservar, la cuenta tiene que tener nombre y apellido de al menos 2 caracteres");
    }
}

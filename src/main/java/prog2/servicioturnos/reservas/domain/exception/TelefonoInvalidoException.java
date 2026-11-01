package prog2.servicioturnos.reservas.domain.exception;

import prog2.servicioturnos.shared.exception.PedidoInvalidoException;

public class TelefonoInvalidoException extends PedidoInvalidoException {

    public TelefonoInvalidoException() {
        super("telefono-invalido",
                "El teléfono tiene que tener entre 7 y 15 dígitos, con un + opcional al principio");
    }
}

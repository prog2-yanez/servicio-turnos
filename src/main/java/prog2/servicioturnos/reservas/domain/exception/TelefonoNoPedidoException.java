package prog2.servicioturnos.reservas.domain.exception;

import prog2.servicioturnos.shared.exception.ConflictoException;

public class TelefonoNoPedidoException extends ConflictoException {

    public TelefonoNoPedidoException() {
        super("telefono-no-pedido", "Esta reserva no está esperando un teléfono en este momento");
    }
}

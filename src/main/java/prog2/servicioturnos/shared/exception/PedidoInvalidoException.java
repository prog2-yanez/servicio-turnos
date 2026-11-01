package prog2.servicioturnos.shared.exception;

// El pedido está bien formado pero no tiene sentido para el negocio, como una fecha que ya pasó. Se responde 400.
public abstract class PedidoInvalidoException extends ErrorDeNegocioException {

    protected PedidoInvalidoException(String codigo, String mensaje) {
        super(codigo, mensaje);
    }
}

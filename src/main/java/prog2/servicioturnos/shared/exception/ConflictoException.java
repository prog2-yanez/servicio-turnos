package prog2.servicioturnos.shared.exception;

// El pedido es válido pero choca con el estado actual: login repetido, proceso abierto, turno ocupado.
public abstract class ConflictoException extends ErrorDeNegocioException {

    protected ConflictoException(String codigo, String mensaje) {
        super(codigo, mensaje);
    }
}

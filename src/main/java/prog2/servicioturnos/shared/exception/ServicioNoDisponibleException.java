package prog2.servicioturnos.shared.exception;

// Un servicio del que depende turnos no responde o falla: el pedido no se puede completar ahora. Se responde 503.
public abstract class ServicioNoDisponibleException extends ErrorDeNegocioException {

    protected ServicioNoDisponibleException(String codigo, String mensaje) {
        super(codigo, mensaje);
    }
}

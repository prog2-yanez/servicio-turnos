package prog2.servicioturnos.reservas.domain.exception;

import prog2.servicioturnos.shared.exception.ServicioNoDisponibleException;

// La cátedra no contestó la cancelación y tampoco se pudo comprobar que la hizo. La reserva sigue confirmada en
// turnos; reintentar es seguro porque la cátedra trata la cancelación como idempotente.
public class CancelacionNoConfirmadaException extends ServicioNoDisponibleException {

    public CancelacionNoConfirmadaException() {
        super("cancelacion-no-confirmada", "No se pudo confirmar la cancelación; probá de nuevo");
    }
}

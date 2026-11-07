package prog2.servicioturnos.reservas.domain.exception;

import prog2.servicioturnos.shared.exception.ServicioNoDisponibleException;

// Kafka no confirmó el envío: el teléfono no salió y el proceso queda como estaba, así se puede reintentar.
public class EnvioNoConfirmadoException extends ServicioNoDisponibleException {

    public EnvioNoConfirmadoException() {
        super("mensajeria-no-disponible", "No se pudo enviar el teléfono en este momento; probá de nuevo");
    }
}

package prog2.servicioturnos.disponibilidad.domain.exception;

import java.time.LocalDate;
import prog2.servicioturnos.shared.exception.PedidoInvalidoException;

public class FechaPasadaException extends PedidoInvalidoException {

    public FechaPasadaException(LocalDate fecha) {
        super("fecha-pasada", "La fecha " + fecha + " ya pasó; elegí hoy o una fecha futura");
    }
}

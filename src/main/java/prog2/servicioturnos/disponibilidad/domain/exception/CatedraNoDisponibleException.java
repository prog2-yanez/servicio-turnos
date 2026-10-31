package prog2.servicioturnos.disponibilidad.domain.exception;

import prog2.servicioturnos.shared.exception.ServicioNoDisponibleException;

public class CatedraNoDisponibleException extends ServicioNoDisponibleException {

    public CatedraNoDisponibleException() {
        super("catedra-no-disponible",
                "El servicio central de turnos no está disponible en este momento; probá de nuevo más tarde");
    }
}

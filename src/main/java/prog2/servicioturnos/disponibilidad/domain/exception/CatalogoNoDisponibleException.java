package prog2.servicioturnos.disponibilidad.domain.exception;

import prog2.servicioturnos.shared.exception.ServicioNoDisponibleException;

public class CatalogoNoDisponibleException extends ServicioNoDisponibleException {

    public CatalogoNoDisponibleException() {
        super("catalogo-no-disponible", "El catálogo no está disponible en este momento; probá de nuevo más tarde");
    }
}

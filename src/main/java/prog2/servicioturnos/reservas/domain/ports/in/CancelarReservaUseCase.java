package prog2.servicioturnos.reservas.domain.ports.in;

import prog2.servicioturnos.reservas.domain.model.ProcesoReserva;

public interface CancelarReservaUseCase {

    // motivo puede ser null.
    ProcesoReserva cancelar(String login, long procesoId, String motivo);
}

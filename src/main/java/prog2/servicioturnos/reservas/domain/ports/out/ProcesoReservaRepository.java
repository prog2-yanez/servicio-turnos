package prog2.servicioturnos.reservas.domain.ports.out;

import java.util.Optional;
import prog2.servicioturnos.reservas.domain.model.ProcesoReserva;

public interface ProcesoReservaRepository {

    boolean tieneProcesoAbierto(long usuarioId);

    Optional<ProcesoReserva> buscarPorId(long id);

    // Los eventos de la cátedra identifican el proceso por su reservationProcessId.
    Optional<ProcesoReserva> buscarPorReservationProcessId(String reservationProcessId);

    // Si al guardar un proceso nuevo el usuario ya tiene uno abierto, lanza ProcesoAbiertoException.
    ProcesoReserva guardar(ProcesoReserva proceso);
}

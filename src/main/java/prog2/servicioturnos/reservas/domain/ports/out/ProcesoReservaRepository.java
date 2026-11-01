package prog2.servicioturnos.reservas.domain.ports.out;

import prog2.servicioturnos.reservas.domain.model.ProcesoReserva;

public interface ProcesoReservaRepository {

    boolean tieneProcesoAbierto(long usuarioId);

    // Si al guardar un proceso nuevo el usuario ya tiene uno abierto, lanza ProcesoAbiertoException.
    ProcesoReserva guardar(ProcesoReserva proceso);
}

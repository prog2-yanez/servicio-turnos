package prog2.servicioturnos.reservas.domain.ports.in;

import prog2.servicioturnos.reservas.domain.model.ProcesoReserva;

public interface EnviarTelefonoUseCase {

    ProcesoReserva enviar(String login, long procesoId, String telefono);
}

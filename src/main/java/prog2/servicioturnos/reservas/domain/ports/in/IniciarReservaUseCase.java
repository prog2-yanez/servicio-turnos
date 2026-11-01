package prog2.servicioturnos.reservas.domain.ports.in;

import prog2.servicioturnos.reservas.domain.model.PedidoReserva;
import prog2.servicioturnos.reservas.domain.model.ProcesoReserva;

public interface IniciarReservaUseCase {

    // Devuelve el proceso ESPERANDO_TELEFONO o, si la cátedra no contestó, INCIERTO.
    ProcesoReserva iniciar(String login, PedidoReserva pedido);
}

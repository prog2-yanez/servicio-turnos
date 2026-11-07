package prog2.servicioturnos.reservas.domain.ports.in;

import prog2.servicioturnos.reservas.domain.model.ReservasPropias;

public interface ListarReservasPropiasUseCase {

    ReservasPropias listar(String login);
}

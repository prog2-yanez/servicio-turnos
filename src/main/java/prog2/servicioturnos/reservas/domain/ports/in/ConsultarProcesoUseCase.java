package prog2.servicioturnos.reservas.domain.ports.in;

import prog2.servicioturnos.reservas.domain.model.ProcesoReserva;

public interface ConsultarProcesoUseCase {

    // Un proceso ajeno se informa como inexistente.
    ProcesoReserva consultar(String login, long procesoId);

    // El único proceso abierto del usuario, para retomarlo sin saber su id.
    ProcesoReserva enCurso(String login);
}

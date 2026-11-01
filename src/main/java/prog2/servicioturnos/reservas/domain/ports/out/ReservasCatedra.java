package prog2.servicioturnos.reservas.domain.ports.out;

import java.util.List;
import prog2.servicioturnos.reservas.domain.model.ReservaCatedra;

// Reservas de la cátedra (contrato, sección 11). La cátedra devuelve las de toda la cuenta técnica, o sea las de
// todos los usuarios de turnos: este puerto devuelve solo las del paciente pedido.
public interface ReservasCatedra {

    // Si la cátedra no responde, lanza CatedraNoDisponibleException.
    List<ReservaCatedra> deUnPaciente(String externalPatientId);
}

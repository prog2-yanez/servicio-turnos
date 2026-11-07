package prog2.servicioturnos.reservas.domain.ports.out;

import java.util.List;
import prog2.servicioturnos.reservas.domain.model.ReservaCatedra;

// Reservas de la cátedra (contrato, secciones 11 y 12). La cátedra devuelve las de toda la cuenta técnica, o sea las
// de todos los usuarios de turnos: este puerto devuelve solo las del paciente pedido.
public interface ReservasCatedra {

    // Si la cátedra no responde, lanza CatedraNoDisponibleException.
    List<ReservaCatedra> deUnPaciente(String externalPatientId);

    // Cancela una reserva confirmada y devuelve su reservationId. Cancelar una ya cancelada no es un error.
    // Lanza ReservaNoCancelableException si la cátedra no la puede cancelar, ResultadoInciertoException si no
    // contestó (pudo haberla cancelado o no) y CatedraNoDisponibleException si rechazó el pedido por otro motivo.
    String cancelar(String reservationProcessId, String motivo);
}

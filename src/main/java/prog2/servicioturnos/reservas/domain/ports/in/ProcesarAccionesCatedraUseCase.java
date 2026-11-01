package prog2.servicioturnos.reservas.domain.ports.in;

import prog2.servicioturnos.reservas.domain.model.EventoCatedra;

// Eventos que la cátedra publica sobre cada proceso de reserva (contrato, sección 15). Cada evento se aplica una
// sola vez: si su eventId ya se procesó, se descarta sin efectos.
public interface ProcesarAccionesCatedraUseCase {

    // AdditionalInformationRequested: la cátedra pide el teléfono del paciente.
    void telefonoPedido(EventoCatedra evento);

    // AdditionalInformationRejected: el teléfono no pasó la validación de la cátedra.
    void telefonoRechazado(EventoCatedra evento);

    // AppointmentConfirmed: la reserva quedó confirmada.
    void confirmado(EventoCatedra evento);

    // AppointmentProcessExpired: venció el hold.
    void vencido(EventoCatedra evento);

    // AppointmentProcessInvalid: la cátedra invalidó el proceso.
    void invalido(EventoCatedra evento);

    // AppointmentCancelled: se canceló la reserva.
    void cancelado(EventoCatedra evento);
}

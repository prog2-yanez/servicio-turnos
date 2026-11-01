package prog2.servicioturnos.reservas.domain.ports.in;

import prog2.servicioturnos.reservas.domain.model.EventoCatedra;

// Eventos que la cátedra publica sobre cada proceso de reserva (contrato, sección 15).
public interface ProcesarAccionesCatedraUseCase {

    // AdditionalInformationRequested: la cátedra pide el teléfono del paciente.
    void telefonoPedido(EventoCatedra evento);

    // AdditionalInformationRejected: el teléfono no pasó la validación de la cátedra.
    void telefonoRechazado(EventoCatedra evento);
}

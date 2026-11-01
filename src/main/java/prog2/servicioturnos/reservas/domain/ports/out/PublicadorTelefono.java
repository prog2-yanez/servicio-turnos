package prog2.servicioturnos.reservas.domain.ports.out;

import prog2.servicioturnos.reservas.domain.model.TelefonoInformado;

public interface PublicadorTelefono {

    // Vuelve recién cuando Kafka confirmó el envío; si no, lanza EnvioNoConfirmadoException.
    void publicar(TelefonoInformado telefono);
}

package prog2.servicioturnos.reservas.domain.ports.out;

// Registro de los eventos de la cátedra ya procesados, para no aplicar dos veces el mismo.
public interface EventosProcesados {

    // Devuelve false si el evento ya estaba registrado: es un duplicado y no hay que aplicarlo.
    boolean registrar(String eventId, String tipo);
}

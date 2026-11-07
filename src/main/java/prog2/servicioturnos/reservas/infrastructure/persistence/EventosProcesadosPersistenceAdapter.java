package prog2.servicioturnos.reservas.infrastructure.persistence;

import java.time.Clock;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import prog2.servicioturnos.reservas.domain.ports.out.EventosProcesados;
import prog2.servicioturnos.reservas.infrastructure.persistence.repository.JpaEventoProcesadoRepository;

@Component
public class EventosProcesadosPersistenceAdapter implements EventosProcesados {

    private final JpaEventoProcesadoRepository eventos;
    private final Clock reloj;

    public EventosProcesadosPersistenceAdapter(JpaEventoProcesadoRepository eventos, Clock reloj) {
        this.eventos = eventos;
        this.reloj = reloj;
    }

    // Se une a la transacción de quien lo llama: el eventId y el cambio del proceso se guardan juntos.
    @Override
    @Transactional
    public boolean registrar(String eventId, String tipo) {
        return eventos.insertarSiNoExiste(eventId, tipo, reloj.instant()) == 1;
    }
}

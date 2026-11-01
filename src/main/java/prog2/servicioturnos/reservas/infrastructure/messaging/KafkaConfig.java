package prog2.servicioturnos.reservas.infrastructure.messaging;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {

    static final long ESPERA_ENTRE_INTENTOS_MS = 5_000;
    static final long REINTENTOS = 3;

    private static final Logger log = LoggerFactory.getLogger(KafkaConfig.class);

    // Reintentos limitados (contrato, sección 17): si un evento sigue fallando se deja en el log y se sigue con el
    // próximo. El proceso queda como estaba y lo revisa la recuperación.
    @Bean
    DefaultErrorHandler manejadorErroresKafka() {
        return new DefaultErrorHandler(KafkaConfig::registrarDescarte,
                new FixedBackOff(ESPERA_ENTRE_INTENTOS_MS, REINTENTOS));
    }

    static void registrarDescarte(ConsumerRecord<?, ?> registro, Exception e) {
        log.error("Se deja el evento del offset {} después de {} reintentos: {}",
                registro.offset(), REINTENTOS, e.getMessage());
    }
}

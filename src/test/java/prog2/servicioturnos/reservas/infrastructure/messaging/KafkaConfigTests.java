package prog2.servicioturnos.reservas.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;

class KafkaConfigTests {

    @Test
    void despuesDeAgotarLosReintentosConfirmaElOffsetParaNoTrabarElTopic() {
        assertThat(new KafkaConfig().manejadorErroresKafka().isAckAfterHandle()).isTrue();
    }

    @Test
    void registrarElDescarteNoFalla() {
        ConsumerRecord<String, String> registro = new ConsumerRecord<>("acciones", 0, 42L, "rp-9", "{}");

        assertThatCode(() -> KafkaConfig.registrarDescarte(registro, new IllegalStateException("base caída")))
                .doesNotThrowAnyException();
    }
}

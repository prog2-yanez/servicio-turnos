package prog2.servicioturnos.reservas.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import prog2.servicioturnos.TestcontainersConfiguration;
import prog2.servicioturnos.reservas.infrastructure.persistence.repository.JpaEventoProcesadoRepository;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, EventosProcesadosPersistenceAdapter.class,
        EventosProcesadosPersistenciaTests.Reloj.class})
class EventosProcesadosPersistenciaTests {

    private static final Instant AHORA = Instant.parse("2026-11-02T12:00:00Z");

    @Autowired
    private EventosProcesadosPersistenceAdapter eventos;

    @Autowired
    private JpaEventoProcesadoRepository repository;

    @Test
    void unEventoNuevoSeRegistraConSuTipoYLaHora() {
        assertThat(eventos.registrar("ev-nuevo", "AppointmentConfirmed")).isTrue();

        assertThat(repository.findById("ev-nuevo")).hasValueSatisfying(evento -> {
            assertThat(evento.getTipo()).isEqualTo("AppointmentConfirmed");
            assertThat(evento.getProcesadoEn()).isEqualTo(AHORA);
        });
    }

    @Test
    void elMismoEventoDosVecesNoFallaYQuedaUnaSolaFila() {
        assertThat(eventos.registrar("ev-repetido", "AppointmentConfirmed")).isTrue();
        assertThat(eventos.registrar("ev-repetido", "AppointmentConfirmed")).isFalse();

        assertThat(repository.findAll()).filteredOn(e -> e.getEventId().equals("ev-repetido")).hasSize(1);
    }

    @TestConfiguration
    static class Reloj {

        @Bean
        Clock reloj() {
            return Clock.fixed(AHORA, ZoneOffset.UTC);
        }
    }
}

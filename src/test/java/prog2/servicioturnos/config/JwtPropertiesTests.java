package prog2.servicioturnos.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class JwtPropertiesTests {

    @Test
    void rechazaUnaClaveVaciaOCorta() {
        assertThatThrownBy(() -> new JwtProperties(null, Duration.ofHours(1)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("JWT_SECRETO");
        assertThatThrownBy(() -> new JwtProperties("corta", Duration.ofHours(1)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void aceptaUnaClaveDeAlMenos256Bits() {
        JwtProperties propiedades = new JwtProperties("x".repeat(32), Duration.ofHours(1));

        assertThat(propiedades.secretoEnBytes()).hasSize(32);
    }
}

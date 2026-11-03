package prog2.servicioturnos.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class CorsPropertiesTests {

    @Test
    void sinConfigurarNoHayOrigenesPermitidos() {
        assertThat(new CorsProperties(null).origenesPermitidos()).isEmpty();
    }

    @Test
    void ignoraLosEspaciosYLosValoresVacios() {
        CorsProperties propiedades = new CorsProperties(List.of(" https://panel.ejemplo.com ", "", "  "));

        assertThat(propiedades.origenesPermitidos()).containsExactly("https://panel.ejemplo.com");
    }
}

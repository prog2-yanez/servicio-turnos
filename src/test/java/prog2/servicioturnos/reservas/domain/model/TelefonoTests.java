package prog2.servicioturnos.reservas.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import prog2.servicioturnos.reservas.domain.exception.TelefonoInvalidoException;

class TelefonoTests {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "+54 261 555-1234 | +542615551234",
        "(0261) 555.1234  | 02615551234",
        "2615551234       | 2615551234",
        "1234567          | 1234567",
        "+123456789012345 | +123456789012345"
    })
    void sacaLosSeparadoresQueAceptaLaCatedra(String escrito, String esperado) {
        assertThat(Telefono.normalizar(escrito).numero()).isEqualTo(esperado);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"123456", "1234567890123456", "261-555-ABCD", "54+2615551234", "++542615551234", "  "})
    void rechazaLoQueNoCumpleElFormato(String escrito) {
        assertThatThrownBy(() -> Telefono.normalizar(escrito)).isInstanceOf(TelefonoInvalidoException.class);
    }
}

package prog2.servicioturnos;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

// Con un origen de navegador permitido (ficticio), como si hubiera un cliente web además de la app Android. Usa los
// endpoints públicos para no depender de un token.
@SpringBootTest(properties = "cors.origenes-permitidos=https://panel.ejemplo.com")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CorsIntegracionTests {

    private static final String PERMITIDO = "https://panel.ejemplo.com";
    private static final String NO_PERMITIDO = "https://otro.ejemplo.com";
    private static final String LOGIN = "/api/v1/cuenta/login";

    @Autowired
    private MockMvcTester mvc;

    @Test
    void unOrigenPermitidoRecibeLosHeadersDeCors() {
        MvcTestResult resultado = mvc.get().uri("/actuator/health").header(HttpHeaders.ORIGIN, PERMITIDO).exchange();

        assertThat(resultado).hasStatusOk();
        assertThat(resultado).hasHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, PERMITIDO);
    }

    // Antes de un POST con JSON el navegador pregunta si puede mandarlo; esa consulta no lleva token.
    @Test
    void laConsultaPreviaDeUnPostConJsonSeRespondeParaUnOrigenPermitido() {
        MvcTestResult resultado = mvc.options().uri(LOGIN)
                .header(HttpHeaders.ORIGIN, PERMITIDO)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type, Authorization").exchange();

        assertThat(resultado).hasStatusOk();
        assertThat(resultado).hasHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, PERMITIDO);
        assertThat(resultado).hasHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET,POST");
    }

    @Test
    void unOrigenNoPermitidoResponde403() {
        MvcTestResult resultado = mvc.post().uri(LOGIN)
                .header(HttpHeaders.ORIGIN, NO_PERMITIDO)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\": \"ana\", \"password\": \"secreta\"}").exchange();

        verificarRechazo(resultado);
    }

    @Test
    void laConsultaPreviaDeUnOrigenNoPermitidoResponde403() {
        MvcTestResult resultado = mvc.options().uri(LOGIN)
                .header(HttpHeaders.ORIGIN, NO_PERMITIDO)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST").exchange();

        verificarRechazo(resultado);
    }

    // La API no tiene PUT ni DELETE: ni siquiera un origen permitido los puede pedir.
    @Test
    void unOrigenPermitidoNoPuedeUsarOtroMetodo() {
        MvcTestResult resultado = mvc.options().uri("/api/v1/reservas/1")
                .header(HttpHeaders.ORIGIN, PERMITIDO)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "DELETE").exchange();

        verificarRechazo(resultado);
    }

    @Test
    void sinOrigenNoCambiaNada() {
        MvcTestResult resultado = mvc.get().uri("/actuator/health").exchange();

        assertThat(resultado).hasStatusOk();
        assertThat(resultado).doesNotContainHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN);
    }

    private static void verificarRechazo(MvcTestResult resultado) {
        assertThat(resultado).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(resultado).hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(resultado).bodyJson().extractingPath("$.type").isEqualTo("urn:turnos:error:origen-no-permitido");
        assertThat(resultado).bodyJson().extractingPath("$.title").isEqualTo("Origen no permitido");
        assertThat(resultado).doesNotContainHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN);
    }
}

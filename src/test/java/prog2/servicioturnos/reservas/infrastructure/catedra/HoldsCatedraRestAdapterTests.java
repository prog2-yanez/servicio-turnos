package prog2.servicioturnos.reservas.infrastructure.catedra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.SocketTimeoutException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.RestClient;
import prog2.servicioturnos.disponibilidad.domain.exception.CatedraNoDisponibleException;
import prog2.servicioturnos.reservas.domain.exception.ProcesoVencidoException;
import prog2.servicioturnos.reservas.domain.exception.ReservaRechazadaException;
import prog2.servicioturnos.reservas.domain.exception.ResultadoInciertoException;
import prog2.servicioturnos.reservas.domain.exception.TurnoNoDisponibleException;
import prog2.servicioturnos.reservas.domain.exception.TurnoOcupadoException;
import prog2.servicioturnos.reservas.domain.model.HoldCreado;
import prog2.servicioturnos.shared.catedra.LectorErrorCatedra;
import tools.jackson.databind.json.JsonMapper;

// La cátedra se simula: cada prueba define qué responde y se verifica cómo lo traduce turnos.
class HoldsCatedraRestAdapterTests {

    private static final String URL_BASE = "http://catedra.test";
    private static final String URL_HOLDS = URL_BASE + "/api/appointment-holds";
    private static final String URL_CONFIRMAR = URL_HOLDS + "/h-77/confirm";
    private static final String JWT_TECNICO = "jwt-tecnico-de-prueba";
    private static final LocalDate MARTES = LocalDate.of(2026, 11, 3);

    private MockRestServiceServer catedra;
    private HoldsCatedraRestAdapter adaptador;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(URL_BASE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + JWT_TECNICO);
        catedra = MockRestServiceServer.bindTo(builder).build();
        adaptador = new HoldsCatedraRestAdapter(builder.build(), new LectorErrorCatedra(JsonMapper.builder().build()));
    }

    @Test
    void creaElHoldConElJwtTecnicoYDevuelveSusDatos() {
        catedra.expect(requestTo(URL_HOLDS))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + JWT_TECNICO))
                .andExpect(content().json("""
                        {"professionalId": 15, "date": "2026-11-03", "startTime": "09:00", "externalPatientId": "4"}
                        """))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body("""
                        {"holdId": 77, "reservationProcessId": "rp-9", "status": "HELD",
                         "expiresAt": "2026-11-02T12:10:00Z", "campoNuevo": "se ignora"}
                        """));

        HoldCreado hold = adaptador.crearHold(15, MARTES, LocalTime.of(9, 0), "4");

        assertThat(hold).isEqualTo(new HoldCreado("77", "rp-9", Instant.parse("2026-11-02T12:10:00Z")));
        catedra.verify();
    }

    @Test
    void confirmaConElNombreYDevuelveElNuevoVencimiento() {
        catedra.expect(requestTo(URL_CONFIRMAR))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {"externalPatientId": "4", "patientFirstName": "Ana", "patientLastName": "Gomez"}
                        """))
                .andRespond(withStatus(HttpStatus.ACCEPTED).contentType(MediaType.APPLICATION_JSON).body("""
                        {"reservationProcessId": "rp-9", "status": "WAITING_FOR_PHONE",
                         "expiresAt": "2026-11-02T12:15:00Z"}
                        """));

        assertThat(adaptador.confirmar("h-77", "4", "Ana", "Gomez")).isEqualTo(Instant.parse("2026-11-02T12:15:00Z"));
    }

    @ParameterizedTest
    @CsvSource({
        "409, SLOT_ALREADY_HELD, ocupado",
        "409, SLOT_ALREADY_RESERVED, ocupado",
        "400, INVALID_SLOT, noDisponible",
        "404, PROFESSIONAL_NOT_FOUND, noDisponible",
        "400, PROFESSIONAL_DISABLED, noDisponible",
        "400, VALIDATION_ERROR, rechazada"
    })
    void traduceLosRechazosDelHold(int estado, String codigo, String esperado) {
        catedra.expect(requestTo(URL_HOLDS)).andRespond(error(estado, codigo));

        Class<? extends RuntimeException> tipo = switch (esperado) {
            case "ocupado" -> TurnoOcupadoException.class;
            case "noDisponible" -> TurnoNoDisponibleException.class;
            default -> ReservaRechazadaException.class;
        };
        assertThatThrownBy(() -> adaptador.crearHold(15, MARTES, LocalTime.of(9, 0), "4")).isInstanceOf(tipo);
    }

    @ParameterizedTest
    @CsvSource({
        "404, HOLD_EXPIRED, vencido",
        "404, HOLD_NOT_FOUND, rechazada",
        "400, EXTERNAL_PATIENT_MISMATCH, rechazada",
        "403, HOLD_OWNER_MISMATCH, cuenta",
        "404, STUDENT_INTEGRATION_NOT_FOUND, cuenta",
        "401, '', cuenta"
    })
    void traduceLosRechazosDeLaConfirmacion(int estado, String codigo, String esperado) {
        catedra.expect(requestTo(URL_CONFIRMAR)).andRespond(error(estado, codigo));

        Class<? extends RuntimeException> tipo = switch (esperado) {
            case "vencido" -> ProcesoVencidoException.class;
            case "cuenta" -> CatedraNoDisponibleException.class;
            default -> ReservaRechazadaException.class;
        };
        assertThatThrownBy(() -> adaptador.confirmar("h-77", "4", "Ana", "Gomez")).isInstanceOf(tipo);
    }

    @Test
    void unTimeoutEsIncierto() {
        catedra.expect(requestTo(URL_HOLDS)).andRespond(withException(new SocketTimeoutException(URL_BASE)));

        assertThatThrownBy(() -> adaptador.crearHold(15, MARTES, LocalTime.of(9, 0), "4"))
                .isInstanceOf(ResultadoInciertoException.class)
                .hasNoCause()
                .message().doesNotContain(URL_BASE, JWT_TECNICO);
    }

    @Test
    void unErrorDelServidorEsInciertoPorqueLaCatedraPudoHaberloHecho() {
        catedra.expect(requestTo(URL_CONFIRMAR)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> adaptador.confirmar("h-77", "4", "Ana", "Gomez"))
                .isInstanceOf(ResultadoInciertoException.class);
    }

    @Test
    void unaRespuestaVaciaOIncompletaEsIncierta() {
        catedra.expect(requestTo(URL_HOLDS)).andRespond(withStatus(HttpStatus.CREATED));
        assertThatThrownBy(() -> adaptador.crearHold(15, MARTES, LocalTime.of(9, 0), "4"))
                .isInstanceOf(ResultadoInciertoException.class);

        catedra.reset();
        catedra.expect(requestTo(URL_HOLDS)).andRespond(withSuccess("""
                {"holdId": 77, "status": "HELD"}
                """, MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> adaptador.crearHold(15, MARTES, LocalTime.of(9, 0), "4"))
                .isInstanceOf(ResultadoInciertoException.class);

        catedra.reset();
        catedra.expect(requestTo(URL_CONFIRMAR)).andRespond(withSuccess("""
                {"status": "WAITING_FOR_PHONE"}
                """, MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> adaptador.confirmar("h-77", "4", "Ana", "Gomez"))
                .isInstanceOf(ResultadoInciertoException.class);
    }

    private static ResponseCreator error(int estado, String codigo) {
        String cuerpo = codigo.isEmpty() ? "" : "{\"status\": " + estado + ", \"code\": \"" + codigo + "\"}";
        return withStatus(HttpStatus.valueOf(estado)).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(cuerpo);
    }
}

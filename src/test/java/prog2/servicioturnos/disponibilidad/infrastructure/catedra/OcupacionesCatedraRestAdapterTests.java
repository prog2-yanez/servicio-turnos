package prog2.servicioturnos.disponibilidad.infrastructure.catedra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
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
import prog2.servicioturnos.disponibilidad.domain.exception.ProfesionalDeshabilitadoException;
import prog2.servicioturnos.disponibilidad.domain.exception.ProfesionalNoEncontradoException;
import prog2.servicioturnos.disponibilidad.domain.model.EstadoOcupacion;
import prog2.servicioturnos.disponibilidad.domain.model.Ocupacion;
import prog2.servicioturnos.shared.catedra.LectorErrorCatedra;
import tools.jackson.databind.json.JsonMapper;

// La cátedra se simula: cada prueba define qué responde y se verifica cómo lo traduce turnos.
class OcupacionesCatedraRestAdapterTests {

    private static final String URL_BASE = "http://catedra.test";
    private static final String JWT_TECNICO = "jwt-tecnico-de-prueba";
    private static final LocalDate FECHA = LocalDate.of(2026, 11, 3);
    private static final String URL_OCUPACIONES =
            URL_BASE + "/api/appointment-occupancies?professionalId=15&from=2026-11-03&to=2026-11-03";

    private MockRestServiceServer catedra;
    private OcupacionesCatedraRestAdapter adaptador;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(URL_BASE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + JWT_TECNICO);
        catedra = MockRestServiceServer.bindTo(builder).build();
        adaptador = new OcupacionesCatedraRestAdapter(
                builder.build(), new LectorErrorCatedra(JsonMapper.builder().build()));
    }

    @Test
    void leeLasOcupacionesConfirmadasYRetenidasConElJwtTecnico() {
        catedra.expect(requestTo(URL_OCUPACIONES))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + JWT_TECNICO))
                .andRespond(withSuccess("""
                        {"professionalId": 15, "from": "2026-11-03", "to": "2026-11-03", "occupancies": [
                          {"date": "2026-11-03", "startTime": "10:30", "status": "HELD",
                           "expiresAt": "2026-11-03T12:05:00Z"},
                          {"date": "2026-11-03", "startTime": "09:00", "status": "CONFIRMED", "expiresAt": null,
                           "campoNuevo": "se ignora"}
                        ]}
                        """, MediaType.APPLICATION_JSON));

        assertThat(adaptador.obtener(15, FECHA)).containsExactly(
                new Ocupacion(FECHA, LocalTime.of(9, 0), EstadoOcupacion.CONFIRMADA, null),
                new Ocupacion(FECHA, LocalTime.of(10, 30), EstadoOcupacion.RETENIDA,
                        Instant.parse("2026-11-03T12:05:00Z")));
        catedra.verify();
    }

    @Test
    void sinOcupacionesDevuelveUnaListaVacia() {
        catedra.expect(requestTo(URL_OCUPACIONES)).andRespond(withSuccess("""
                {"professionalId": 15, "from": "2026-11-03", "to": "2026-11-03", "occupancies": []}
                """, MediaType.APPLICATION_JSON));

        assertThat(adaptador.obtener(15, FECHA)).isEmpty();
    }

    @Test
    void siUnTurnoApareceDosVecesPrevaleceElConfirmado() {
        catedra.expect(requestTo(URL_OCUPACIONES)).andRespond(withSuccess("""
                {"occupancies": [
                  {"date": "2026-11-03", "startTime": "09:00", "status": "CONFIRMED"},
                  {"date": "2026-11-03", "startTime": "09:00", "status": "HELD",
                   "expiresAt": "2026-11-03T12:05:00Z"},
                  {"date": "2026-11-03", "startTime": "11:00", "status": "HELD",
                   "expiresAt": "2026-11-03T12:05:00Z"},
                  {"date": "2026-11-03", "startTime": "11:00", "status": "CONFIRMED"}
                ]}
                """, MediaType.APPLICATION_JSON));

        assertThat(adaptador.obtener(15, FECHA))
                .extracting(Ocupacion::horaInicio, Ocupacion::estado)
                .containsExactly(
                tuple(LocalTime.of(9, 0), EstadoOcupacion.CONFIRMADA),
                tuple(LocalTime.of(11, 0), EstadoOcupacion.CONFIRMADA));
    }

    @Test
    void ignoraLasOcupacionesIncompletasOConEstadoDesconocido() {
        catedra.expect(requestTo(URL_OCUPACIONES)).andRespond(withSuccess("""
                {"occupancies": [
                  {"date": "2026-11-03", "startTime": "09:00", "status": "OTRO"},
                  {"date": "2026-11-03", "status": "CONFIRMED"},
                  {"date": "2026-11-03", "startTime": "10:00", "status": "CONFIRMED"}
                ]}
                """, MediaType.APPLICATION_JSON));

        assertThat(adaptador.obtener(15, FECHA))
                .extracting(Ocupacion::horaInicio)
                .containsExactly(LocalTime.of(10, 0));
    }

    @Test
    void unaRespuestaSinOcupacionesEsUnaListaVacia() {
        catedra.expect(requestTo(URL_OCUPACIONES)).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThat(adaptador.obtener(15, FECHA)).isEmpty();
    }

    @Test
    void profesionalNoEncontradoEnLaCatedraEs404() {
        catedra.expect(requestTo(URL_OCUPACIONES))
                .andRespond(error(HttpStatus.NOT_FOUND, "PROFESSIONAL_NOT_FOUND"));

        assertThatThrownBy(() -> adaptador.obtener(15, FECHA)).isInstanceOf(ProfesionalNoEncontradoException.class);
    }

    @Test
    void profesionalDeshabilitadoEnLaCatedraEs409() {
        catedra.expect(requestTo(URL_OCUPACIONES))
                .andRespond(error(HttpStatus.BAD_REQUEST, "PROFESSIONAL_DISABLED"));

        assertThatThrownBy(() -> adaptador.obtener(15, FECHA)).isInstanceOf(ProfesionalDeshabilitadoException.class);
    }

    // La cuenta técnica mal configurada, un pedido mal armado o una falla de la cátedra no son culpa del usuario.
    @ParameterizedTest
    @CsvSource({
        "404, STUDENT_INTEGRATION_NOT_FOUND",
        "403, STUDENT_INTEGRATION_NOT_FOUND",
        "400, INVALID_DATE_RANGE",
        "400, VALIDATION_ERROR",
        "500, ''",
        "503, ''"
    })
    void otrosErroresDeLaCatedraSonCatedraNoDisponible(int estado, String codigo) {
        catedra.expect(requestTo(URL_OCUPACIONES)).andRespond(error(HttpStatus.valueOf(estado), codigo));

        assertThatThrownBy(() -> adaptador.obtener(15, FECHA)).isInstanceOf(CatedraNoDisponibleException.class);
    }

    @Test
    void un401SinCuerpoEsCatedraNoDisponible() {
        catedra.expect(requestTo(URL_OCUPACIONES)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> adaptador.obtener(15, FECHA)).isInstanceOf(CatedraNoDisponibleException.class);
    }

    @Test
    void siLaCatedraNoRespondeATiempoEsCatedraNoDisponibleSinMostrarLaUrl() {
        catedra.expect(requestTo(URL_OCUPACIONES))
                .andRespond(withException(new SocketTimeoutException(URL_BASE)));

        assertThatThrownBy(() -> adaptador.obtener(15, FECHA))
                .isInstanceOf(CatedraNoDisponibleException.class)
                .hasNoCause()
                .message().doesNotContain(URL_BASE, JWT_TECNICO);
    }

    @Test
    void unaRespuestaQueNoSePuedeLeerEsCatedraNoDisponible() {
        catedra.expect(requestTo(URL_OCUPACIONES))
                .andRespond(withSuccess("{esto no es json", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> adaptador.obtener(15, FECHA)).isInstanceOf(CatedraNoDisponibleException.class);
    }

    private static ResponseCreator error(HttpStatus estado, String codigo) {
        String cuerpo = codigo.isEmpty() ? "{}" : "{\"status\": " + estado.value() + ", \"code\": \"" + codigo + "\"}";
        return withStatus(estado).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(cuerpo);
    }
}

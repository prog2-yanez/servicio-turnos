package prog2.servicioturnos.reservas.infrastructure.catedra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import prog2.servicioturnos.disponibilidad.domain.exception.CatedraNoDisponibleException;
import prog2.servicioturnos.reservas.domain.model.EstadoReservaCatedra;
import prog2.servicioturnos.reservas.domain.model.ReservaCatedra;
import prog2.servicioturnos.shared.catedra.LectorErrorCatedra;
import tools.jackson.databind.json.JsonMapper;

class ReservasCatedraRestAdapterTests {

    private static final String URL_BASE = "http://catedra.test";
    private static final String JWT_TECNICO = "jwt-tecnico-de-prueba";
    private static final String URL_PAGINA_0 = URL_BASE + "/api/appointments?externalPatientId=4&page=0&size=100";
    private static final String URL_PAGINA_1 = URL_BASE + "/api/appointments?externalPatientId=4&page=1&size=100";

    private MockRestServiceServer catedra;
    private ReservasCatedraRestAdapter adaptador;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(URL_BASE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + JWT_TECNICO);
        catedra = MockRestServiceServer.bindTo(builder).build();
        adaptador = new ReservasCatedraRestAdapter(builder.build(),
                new LectorErrorCatedra(JsonMapper.builder().build()));
    }

    @Test
    void traeLasReservasDelPacienteConElJwtTecnicoSinDatosDelPaciente() {
        catedra.expect(requestTo(URL_PAGINA_0))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + JWT_TECNICO))
                .andRespond(withSuccess("[" + reserva(500, "rp-9", "4", "CONFIRMED") + "]",
                        MediaType.APPLICATION_JSON));

        List<ReservaCatedra> reservas = adaptador.deUnPaciente("4");

        assertThat(reservas).containsExactly(new ReservaCatedra("500", "rp-9", "4", LocalDate.of(2026, 11, 3),
                LocalTime.of(9, 0), LocalTime.of(9, 30), 15, "Ana", "Gomez", EstadoReservaCatedra.CONFIRMADA,
                Instant.parse("2026-11-02T12:05:00Z"), null, null));
        catedra.verify();
    }

    @Test
    void nuncaDevuelveReservasDeOtroPacienteAunqueLaCatedraLasMande() {
        catedra.expect(requestTo(URL_PAGINA_0)).andRespond(withSuccess("[" + reserva(500, "rp-9", "4", "CONFIRMED")
                + "," + reserva(501, "rp-10", "5", "CONFIRMED") + "," + reserva(502, "rp-11", null, "CONFIRMED")
                + "]", MediaType.APPLICATION_JSON));

        assertThat(adaptador.deUnPaciente("4")).extracting(ReservaCatedra::reservationId).containsExactly("500");
    }

    @Test
    void recorreTodasLasPaginas() {
        String paginaLlena = IntStream.range(0, 100)
                .mapToObj(i -> reserva(1000 + i, "rp-" + i, "4", "CONFIRMED"))
                .collect(Collectors.joining(",", "[", "]"));
        catedra.expect(requestTo(URL_PAGINA_0)).andRespond(withSuccess(paginaLlena, MediaType.APPLICATION_JSON));
        catedra.expect(requestTo(URL_PAGINA_1)).andRespond(withSuccess(
                "[" + reserva(2000, "rp-ultima", "4", "CANCELLED") + "]", MediaType.APPLICATION_JSON));

        List<ReservaCatedra> reservas = adaptador.deUnPaciente("4");

        assertThat(reservas).hasSize(101);
        assertThat(reservas.getLast().estado()).isEqualTo(EstadoReservaCatedra.CANCELADA);
        catedra.verify();
    }

    @Test
    void traduceCadaEstadoEIgnoraLosDesconocidosOIncompletos() {
        catedra.expect(requestTo(URL_PAGINA_0)).andRespond(withSuccess("["
                + reserva(1, "rp-1", "4", "PHONE_PENDING") + "," + reserva(2, "rp-2", "4", "FAILED") + ","
                + reserva(3, "rp-3", "4", "OTRO") + ","
                + "{\"reservationId\": 4, \"externalPatientId\": \"4\", \"status\": \"CONFIRMED\"}"
                + "]", MediaType.APPLICATION_JSON));

        assertThat(adaptador.deUnPaciente("4")).extracting(ReservaCatedra::estado)
                .containsExactly(EstadoReservaCatedra.TELEFONO_PENDIENTE, EstadoReservaCatedra.FALLIDA);
    }

    @Test
    void sinReservasDevuelveUnaListaVacia() {
        catedra.expect(requestTo(URL_PAGINA_0)).andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(adaptador.deUnPaciente("4")).isEmpty();
    }

    @Test
    void unaRespuestaSinCuerpoEsUnaListaVacia() {
        catedra.expect(requestTo(URL_PAGINA_0)).andRespond(withSuccess());

        assertThat(adaptador.deUnPaciente("4")).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 403, 500, 503, 400})
    void unErrorDeLaCatedraEsCatedraNoDisponible(int estado) {
        catedra.expect(requestTo(URL_PAGINA_0)).andRespond(withStatus(HttpStatus.valueOf(estado)));

        assertThatThrownBy(() -> adaptador.deUnPaciente("4")).isInstanceOf(CatedraNoDisponibleException.class);
    }

    @Test
    void unTimeoutEsCatedraNoDisponibleSinMostrarLaUrl() {
        catedra.expect(requestTo(URL_PAGINA_0)).andRespond(withException(new SocketTimeoutException(URL_BASE)));

        assertThatThrownBy(() -> adaptador.deUnPaciente("4"))
                .isInstanceOf(CatedraNoDisponibleException.class)
                .hasNoCause()
                .message().doesNotContain(URL_BASE, JWT_TECNICO);
    }

    private static String reserva(long reservationId, String reservationProcessId, String paciente, String estado) {
        String externalPatientId = paciente == null ? "null" : "\"" + paciente + "\"";
        return """
                {"reservationId": %d, "reservationProcessId": "%s", "externalPatientId": %s,
                 "appointmentDate": "2026-11-03", "startTime": "09:00", "endTime": "09:30", "professionalId": 15,
                 "professionalFirstName": "Ana", "professionalLastName": "Gomez", "patientFirstName": "Paciente",
                 "patientLastName": "De Prueba", "patientPhone": "+5490000000", "status": "%s",
                 "createdAt": "2026-11-02T12:00:00Z", "confirmedAt": "2026-11-02T12:05:00Z", "cancelledAt": null,
                 "cancellationReason": null}
                """.formatted(reservationId, reservationProcessId, externalPatientId, estado);
    }
}

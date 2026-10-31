package prog2.servicioturnos.disponibilidad.infrastructure.catalogo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.SocketTimeoutException;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import prog2.servicioturnos.disponibilidad.domain.exception.CatalogoNoDisponibleException;
import prog2.servicioturnos.disponibilidad.domain.exception.ProfesionalDeshabilitadoException;
import prog2.servicioturnos.disponibilidad.domain.exception.ProfesionalNoEncontradoException;
import prog2.servicioturnos.disponibilidad.domain.model.HorarioSemanal;
import prog2.servicioturnos.disponibilidad.domain.model.ProfesionalConAgenda;

// El catálogo se simula: cada prueba define qué responde y se verifica cómo lo traduce turnos.
class CatalogoRestAdapterTests {

    private static final String URL_BASE = "http://catalogo.test";
    private static final String URL_PROFESIONAL = URL_BASE + "/api/v1/profesionales/15";
    private static final String TOKEN = "token-de-prueba";

    private static final String PROFESIONAL = """
            {
              "id": 15, "categoriaId": 1, "nombre": "Ana", "apellido": "Gomez", "habilitado": true,
              "horarios": [
                {"id": 21, "diaSemana": "MONDAY", "horaInicio": "09:00", "horaFin": "12:00",
                 "duracionTurnoMinutos": 30}
              ],
              "versionCatalogo": 7,
              "campoNuevo": "se ignora"
            }
            """;

    private MockRestServiceServer catalogo;
    private CatalogoRestAdapter adaptador;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder().baseUrl(URL_BASE);
        catalogo = MockRestServiceServer.bindTo(builder).build();
        adaptador = new CatalogoRestAdapter(builder.build());
        conUsuarioAutenticado();
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void traeElProfesionalConSuAgendaReenviandoElTokenDelUsuario() {
        catalogo.expect(requestTo(URL_PROFESIONAL))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andRespond(withSuccess(PROFESIONAL, MediaType.APPLICATION_JSON));

        ProfesionalConAgenda profesional = adaptador.obtenerProfesional(15);

        assertThat(profesional).isEqualTo(new ProfesionalConAgenda(15, 1, "Ana", "Gomez",
                List.of(new HorarioSemanal(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0), 30)), 7));
        catalogo.verify();
    }

    @Test
    void sinUsuarioAutenticadoPideSinToken() {
        SecurityContextHolder.clearContext();
        catalogo.expect(requestTo(URL_PROFESIONAL))
                .andExpect(headerDoesNotExist(HttpHeaders.AUTHORIZATION))
                .andRespond(withSuccess(PROFESIONAL, MediaType.APPLICATION_JSON));

        assertThat(adaptador.obtenerProfesional(15).id()).isEqualTo(15);
    }

    @Test
    void unProfesionalSinHorariosQuedaConLaAgendaVacia() {
        catalogo.expect(requestTo(URL_PROFESIONAL)).andRespond(withSuccess("""
                {"id": 15, "categoriaId": 1, "nombre": "Ana", "apellido": "Gomez", "versionCatalogo": 7}
                """, MediaType.APPLICATION_JSON));

        assertThat(adaptador.obtenerProfesional(15).horarios()).isEmpty();
    }

    @Test
    void un404DelCatalogoEsProfesionalNoEncontrado() {
        catalogo.expect(requestTo(URL_PROFESIONAL)).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> adaptador.obtenerProfesional(15))
                .isInstanceOf(ProfesionalNoEncontradoException.class)
                .hasMessage("No existe un profesional con id 15");
    }

    @Test
    void un409DelCatalogoEsProfesionalDeshabilitado() {
        catalogo.expect(requestTo(URL_PROFESIONAL)).andRespond(withStatus(HttpStatus.CONFLICT));

        assertThatThrownBy(() -> adaptador.obtenerProfesional(15))
                .isInstanceOf(ProfesionalDeshabilitadoException.class)
                .hasMessage("El profesional con id 15 está deshabilitado");
    }

    @ParameterizedTest
    @EnumSource(value = HttpStatus.class,
            names = {"INTERNAL_SERVER_ERROR", "SERVICE_UNAVAILABLE", "UNAUTHORIZED", "FORBIDDEN", "BAD_REQUEST"})
    void otrosErroresDelCatalogoSonCatalogoNoDisponible(HttpStatus estado) {
        catalogo.expect(requestTo(URL_PROFESIONAL)).andRespond(withStatus(estado));

        assertThatThrownBy(() -> adaptador.obtenerProfesional(15)).isInstanceOf(CatalogoNoDisponibleException.class);
    }

    @Test
    void siElCatalogoNoRespondeATiempoEsCatalogoNoDisponible() {
        catalogo.expect(requestTo(URL_PROFESIONAL)).andRespond(withException(new SocketTimeoutException("timeout")));

        assertThatThrownBy(() -> adaptador.obtenerProfesional(15)).isInstanceOf(CatalogoNoDisponibleException.class);
    }

    @Test
    void unaRespuestaQueNoSePuedeLeerEsCatalogoNoDisponible() {
        catalogo.expect(requestTo(URL_PROFESIONAL))
                .andRespond(withSuccess("{esto no es json", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> adaptador.obtenerProfesional(15)).isInstanceOf(CatalogoNoDisponibleException.class);
    }

    @Test
    void unaRespuestaSinCuerpoEsCatalogoNoDisponible() {
        catalogo.expect(requestTo(URL_PROFESIONAL)).andRespond(withSuccess());

        assertThatThrownBy(() -> adaptador.obtenerProfesional(15)).isInstanceOf(CatalogoNoDisponibleException.class);
    }

    private static void conUsuarioAutenticado() {
        Jwt jwt = Jwt.withTokenValue(TOKEN)
                .header("alg", "HS256")
                .subject("ana")
                .issuedAt(Instant.parse("2026-10-31T12:00:00Z"))
                .expiresAt(Instant.parse("2026-10-31T13:00:00Z"))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}

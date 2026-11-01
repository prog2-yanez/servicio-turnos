package prog2.servicioturnos.reservas.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import prog2.servicioturnos.config.JwtConfig;
import prog2.servicioturnos.config.SecurityConfig;
import prog2.servicioturnos.reservas.domain.exception.DatosDeCuentaIncompletosException;
import prog2.servicioturnos.reservas.domain.exception.ProcesoAbiertoException;
import prog2.servicioturnos.reservas.domain.exception.TurnoOcupadoException;
import prog2.servicioturnos.reservas.domain.model.EstadoProceso;
import prog2.servicioturnos.reservas.domain.model.PedidoReserva;
import prog2.servicioturnos.reservas.domain.model.ProcesoReserva;
import prog2.servicioturnos.reservas.domain.ports.in.IniciarReservaUseCase;

@WebMvcTest(controllers = ReservaController.class)
@Import({SecurityConfig.class, JwtConfig.class})
class ReservaControllerTests {

    private static final String CUERPO = """
            {"profesionalId": 15, "fecha": "2026-11-03", "horaInicio": "09:00"}
            """;
    private static final PedidoReserva PEDIDO = new PedidoReserva(15, LocalDate.of(2026, 11, 3), LocalTime.of(9, 0));

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private IniciarReservaUseCase iniciarReserva;

    @Test
    void esperandoElTelefonoResponde201ConElProcesoSinLosIdsDeLaCatedra() {
        when(iniciarReserva.iniciar("ana", PEDIDO)).thenReturn(proceso(EstadoProceso.ESPERANDO_TELEFONO));

        MvcTestResult resultado = reservar(CUERPO);

        assertThat(resultado).hasStatus(HttpStatus.CREATED);
        assertThat(resultado).bodyJson().isEqualTo("""
                {"id": 100, "estado": "ESPERANDO_TELEFONO", "profesionalId": 15, "fecha": "2026-11-03",
                 "horaInicio": "09:00", "venceEn": "2026-11-02T12:15:00Z"}
                """);
        assertThat(resultado).bodyText().doesNotContain("h-77", "rp-9");
    }

    @Test
    void inciertoResponde202() {
        when(iniciarReserva.iniciar("ana", PEDIDO)).thenReturn(proceso(EstadoProceso.INCIERTO));

        MvcTestResult resultado = reservar(CUERPO);

        assertThat(resultado).hasStatus(HttpStatus.ACCEPTED);
        assertThat(resultado).bodyJson().extractingPath("$.estado").isEqualTo("INCIERTO");
    }

    @Test
    void sinTokenResponde401() {
        MvcTestResult resultado = mvc.post().uri("/api/v1/reservas")
                .contentType(MediaType.APPLICATION_JSON).content(CUERPO).exchange();

        assertThat(resultado).hasStatus(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(iniciarReserva);
    }

    @Test
    void sinLosDatosDelTurnoResponde400NombrandoCadaCampo() {
        MvcTestResult resultado = reservar("{}");

        verificarProblema(resultado, HttpStatus.BAD_REQUEST, "datos-invalidos");
        assertThat(resultado).bodyJson().extractingPath("$.detail").asString()
                .contains("profesionalId", "fecha", "horaInicio");
        verifyNoInteractions(iniciarReserva);
    }

    @Test
    void conUnProcesoAbiertoResponde409() {
        when(iniciarReserva.iniciar("ana", PEDIDO)).thenThrow(new ProcesoAbiertoException());

        verificarProblema(reservar(CUERPO), HttpStatus.CONFLICT, "proceso-abierto");
    }

    @Test
    void unTurnoOcupadoResponde409() {
        when(iniciarReserva.iniciar("ana", PEDIDO)).thenThrow(new TurnoOcupadoException());

        verificarProblema(reservar(CUERPO), HttpStatus.CONFLICT, "turno-ocupado");
    }

    @Test
    void unaCuentaSinNombreResponde400() {
        when(iniciarReserva.iniciar(any(), any())).thenThrow(new DatosDeCuentaIncompletosException());

        verificarProblema(reservar(CUERPO), HttpStatus.BAD_REQUEST, "datos-de-cuenta-incompletos");
    }

    private MvcTestResult reservar(String cuerpo) {
        return mvc.post().uri("/api/v1/reservas").with(usuario())
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo).exchange();
    }

    private static ProcesoReserva proceso(EstadoProceso estado) {
        return new ProcesoReserva(100L, 4, 15, LocalDate.of(2026, 11, 3), LocalTime.of(9, 0), 7, estado, "h-77",
                "rp-9", null, Instant.parse("2026-11-02T12:15:00Z"), Instant.parse("2026-11-02T12:00:00Z"),
                Instant.parse("2026-11-02T12:00:05Z"));
    }

    private static void verificarProblema(MvcTestResult resultado, HttpStatus estado, String codigo) {
        assertThat(resultado).hasStatus(estado);
        assertThat(resultado).hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(resultado).bodyJson().extractingPath("$.type").isEqualTo("urn:turnos:error:" + codigo);
    }

    private static RequestPostProcessor usuario() {
        return jwt().jwt(token -> token.subject("ana")).authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }
}

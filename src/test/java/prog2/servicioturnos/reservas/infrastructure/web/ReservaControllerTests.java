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
import prog2.servicioturnos.reservas.domain.exception.ProcesoNoEncontradoException;
import prog2.servicioturnos.reservas.domain.exception.SinProcesoEnCursoException;
import prog2.servicioturnos.reservas.domain.exception.ProcesoAbiertoException;
import prog2.servicioturnos.reservas.domain.exception.TelefonoInvalidoException;
import prog2.servicioturnos.reservas.domain.exception.TelefonoNoPedidoException;
import prog2.servicioturnos.reservas.domain.exception.TurnoOcupadoException;
import prog2.servicioturnos.reservas.domain.model.EstadoProceso;
import prog2.servicioturnos.reservas.domain.model.PedidoReserva;
import prog2.servicioturnos.reservas.domain.model.ProcesoReserva;
import prog2.servicioturnos.reservas.domain.ports.in.ConsultarProcesoUseCase;
import prog2.servicioturnos.reservas.domain.ports.in.EnviarTelefonoUseCase;
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

    @MockitoBean
    private EnviarTelefonoUseCase enviarTelefono;

    @MockitoBean
    private ConsultarProcesoUseCase consultarProceso;

    @Test
    void esperandoElTelefonoResponde201ConElProcesoSinLosIdsDeLaCatedra() {
        when(iniciarReserva.iniciar("ana", PEDIDO)).thenReturn(proceso(EstadoProceso.ESPERANDO_TELEFONO));

        MvcTestResult resultado = reservar(CUERPO);

        assertThat(resultado).hasStatus(HttpStatus.CREATED);
        assertThat(resultado).bodyJson().isEqualTo("""
                {"id": 100, "estado": "ESPERANDO_TELEFONO", "abierto": true, "profesionalId": 15,
                 "fecha": "2026-11-03", "horaInicio": "09:00", "venceEn": "2026-11-02T12:15:00Z", "motivo": null,
                 "puedeEnviarTelefono": false}
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

    @Test
    void elTelefonoEnviadoResponde202ConElProceso() {
        when(enviarTelefono.enviar("ana", 100, "+54 261 555-1234"))
                .thenReturn(proceso(EstadoProceso.TELEFONO_ENVIADO));

        MvcTestResult resultado = mandarTelefono(100, "{\"telefono\": \"+54 261 555-1234\"}");

        assertThat(resultado).hasStatus(HttpStatus.ACCEPTED);
        assertThat(resultado).bodyJson().extractingPath("$.estado").isEqualTo("TELEFONO_ENVIADO");
    }

    @Test
    void sinTelefonoResponde400SinLlamarAlCasoDeUso() {
        verificarProblema(mandarTelefono(100, "{\"telefono\": \" \"}"), HttpStatus.BAD_REQUEST, "datos-invalidos");
        verifyNoInteractions(enviarTelefono);
    }

    @Test
    void losErroresDelTelefonoSeTraducenASuStatus() {
        when(enviarTelefono.enviar("ana", 999, "2615551234")).thenThrow(new ProcesoNoEncontradoException(999));
        when(enviarTelefono.enviar("ana", 101, "2615551234")).thenThrow(new TelefonoNoPedidoException());
        when(enviarTelefono.enviar("ana", 100, "123")).thenThrow(new TelefonoInvalidoException());

        verificarProblema(mandarTelefono(999, "{\"telefono\": \"2615551234\"}"),
                HttpStatus.NOT_FOUND, "proceso-no-encontrado");
        verificarProblema(mandarTelefono(101, "{\"telefono\": \"2615551234\"}"),
                HttpStatus.CONFLICT, "telefono-no-pedido");
        verificarProblema(mandarTelefono(100, "{\"telefono\": \"123\"}"), HttpStatus.BAD_REQUEST, "telefono-invalido");
    }

    @Test
    void mandarElTelefonoSinTokenResponde401() {
        MvcTestResult resultado = mvc.post().uri("/api/v1/reservas/100/telefono")
                .contentType(MediaType.APPLICATION_JSON).content("{\"telefono\": \"2615551234\"}").exchange();

        assertThat(resultado).hasStatus(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(enviarTelefono);
    }

    @Test
    void consultaUnProcesoPropioConQuePuedeHacer() {
        ProcesoReserva conPedido = proceso(EstadoProceso.ESPERANDO_TELEFONO).telefonoPedido("ev-pedido",
                Instant.parse("2026-11-02T12:20:00Z"), Instant.parse("2026-11-02T12:01:00Z"));
        when(consultarProceso.consultar("ana", 100)).thenReturn(conPedido);

        MvcTestResult resultado = mvc.get().uri("/api/v1/reservas/100").with(usuario()).exchange();

        assertThat(resultado).hasStatusOk();
        assertThat(resultado).bodyJson().isStrictlyEqualTo("""
                {"id": 100, "estado": "ESPERANDO_TELEFONO", "abierto": true, "profesionalId": 15,
                 "fecha": "2026-11-03", "horaInicio": "09:00", "venceEn": "2026-11-02T12:20:00Z", "motivo": null,
                 "puedeEnviarTelefono": true}
                """);
    }

    @Test
    void unProcesoTerminadoNoMuestraVencimientoYSiElMotivo() {
        ProcesoReserva cancelado = proceso(EstadoProceso.ESPERANDO_TELEFONO)
                .cancelado("res-500", "Pedido del paciente", Instant.parse("2026-11-02T12:05:00Z"));
        when(consultarProceso.consultar("ana", 100)).thenReturn(cancelado);

        MvcTestResult resultado = mvc.get().uri("/api/v1/reservas/100").with(usuario()).exchange();

        assertThat(resultado).bodyJson().extractingPath("$.estado").isEqualTo("CANCELADO");
        assertThat(resultado).bodyJson().extractingPath("$.abierto").isEqualTo(false);
        assertThat(resultado).bodyJson().extractingPath("$.venceEn").isNull();
        assertThat(resultado).bodyJson().extractingPath("$.motivo").isEqualTo("Pedido del paciente");
        assertThat(resultado).bodyText().doesNotContain("res-500");
    }

    @Test
    void unProcesoAjenoOInexistenteResponde404() {
        when(consultarProceso.consultar("ana", 999)).thenThrow(new ProcesoNoEncontradoException(999));

        verificarProblema(mvc.get().uri("/api/v1/reservas/999").with(usuario()).exchange(),
                HttpStatus.NOT_FOUND, "proceso-no-encontrado");
    }

    @Test
    void elProcesoEnCursoSeConsultaSinSaberSuId() {
        when(consultarProceso.enCurso("ana")).thenReturn(proceso(EstadoProceso.INCIERTO));

        MvcTestResult resultado = mvc.get().uri("/api/v1/reservas/en-curso").with(usuario()).exchange();

        assertThat(resultado).hasStatusOk();
        assertThat(resultado).bodyJson().extractingPath("$.id").isEqualTo(100);
        assertThat(resultado).bodyJson().extractingPath("$.estado").isEqualTo("INCIERTO");
    }

    @Test
    void sinProcesoEnCursoResponde404() {
        when(consultarProceso.enCurso("ana")).thenThrow(new SinProcesoEnCursoException());

        verificarProblema(mvc.get().uri("/api/v1/reservas/en-curso").with(usuario()).exchange(),
                HttpStatus.NOT_FOUND, "sin-proceso-en-curso");
    }

    @Test
    void consultarSinTokenResponde401() {
        assertThat(mvc.get().uri("/api/v1/reservas/100").exchange()).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(mvc.get().uri("/api/v1/reservas/en-curso").exchange()).hasStatus(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(consultarProceso);
    }

    private MvcTestResult mandarTelefono(long id, String cuerpo) {
        return mvc.post().uri("/api/v1/reservas/" + id + "/telefono").with(usuario())
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo).exchange();
    }

    private MvcTestResult reservar(String cuerpo) {
        return mvc.post().uri("/api/v1/reservas").with(usuario())
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo).exchange();
    }

    private static ProcesoReserva proceso(EstadoProceso estado) {
        return new ProcesoReserva(100L, 4, 15, LocalDate.of(2026, 11, 3), LocalTime.of(9, 0), 7, estado, "h-77",
                "rp-9", null, null, null, Instant.parse("2026-11-02T12:15:00Z"), Instant.parse("2026-11-02T12:00:00Z"),
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

package prog2.servicioturnos.disponibilidad.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
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
import prog2.servicioturnos.disponibilidad.domain.exception.CatalogoNoDisponibleException;
import prog2.servicioturnos.disponibilidad.domain.exception.FechaPasadaException;
import prog2.servicioturnos.disponibilidad.domain.exception.ProfesionalNoEncontradoException;
import prog2.servicioturnos.disponibilidad.domain.model.ProfesionalConTurnosLibres;
import prog2.servicioturnos.disponibilidad.domain.model.TurnoLibre;
import prog2.servicioturnos.disponibilidad.domain.model.TurnosLibresDelProfesional;
import prog2.servicioturnos.disponibilidad.domain.ports.in.BuscarProfesionalesConTurnosLibresUseCase;
import prog2.servicioturnos.disponibilidad.domain.ports.in.ConsultarTurnosLibresUseCase;

@WebMvcTest(controllers = DisponibilidadController.class)
@Import({SecurityConfig.class, JwtConfig.class})
class DisponibilidadControllerTests {

    private static final LocalDate MARTES = LocalDate.of(2026, 11, 3);

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private ConsultarTurnosLibresUseCase consultarTurnosLibres;

    @MockitoBean
    private BuscarProfesionalesConTurnosLibresUseCase buscarConTurnosLibres;

    @Test
    void devuelveLosTurnosLibresDelProfesional() {
        when(consultarTurnosLibres.consultar(15, MARTES)).thenReturn(new TurnosLibresDelProfesional(15, MARTES, 7,
                List.of(new TurnoLibre(LocalTime.of(9, 0), LocalTime.of(9, 30)))));

        MvcTestResult resultado = mvc.get().uri("/api/v1/disponibilidad/profesionales/15?fecha=2026-11-03")
                .with(usuario()).exchange();

        assertThat(resultado).hasStatusOk();
        assertThat(resultado).bodyJson().isEqualTo("""
                {"profesionalId": 15, "fecha": "2026-11-03", "versionCatalogo": 7,
                 "turnos": [{"horaInicio": "09:00", "horaFin": "09:30"}]}
                """);
    }

    @Test
    void devuelveLosProfesionalesConTurnosLibres() {
        when(buscarConTurnosLibres.buscar(1, MARTES))
                .thenReturn(List.of(new ProfesionalConTurnosLibres(15, 1, "Ana", "Gomez", 4)));

        MvcTestResult resultado = mvc.get().uri("/api/v1/disponibilidad/profesionales?categoriaId=1&fecha=2026-11-03")
                .with(usuario()).exchange();

        assertThat(resultado).hasStatusOk();
        assertThat(resultado).bodyJson().isEqualTo("""
                [{"id": 15, "categoriaId": 1, "nombre": "Ana", "apellido": "Gomez", "turnosLibres": 4}]
                """);
    }

    @Test
    void sinTokenDevuelve401() {
        MvcTestResult resultado = mvc.get().uri("/api/v1/disponibilidad/profesionales/15?fecha=2026-11-03").exchange();

        assertThat(resultado).hasStatus(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(consultarTurnosLibres);
    }

    @Test
    void unaFechaPasadaDevuelve400ConSuTipo() {
        when(consultarTurnosLibres.consultar(15, MARTES)).thenThrow(new FechaPasadaException(MARTES));

        MvcTestResult resultado = mvc.get().uri("/api/v1/disponibilidad/profesionales/15?fecha=2026-11-03")
                .with(usuario()).exchange();

        verificarProblema(resultado, HttpStatus.BAD_REQUEST, "fecha-pasada");
    }

    @Test
    void sinFechaOConUnaFechaMalEscritaDevuelve400() {
        verificarProblema(mvc.get().uri("/api/v1/disponibilidad/profesionales/15").with(usuario()).exchange(),
                HttpStatus.BAD_REQUEST, "parametro-invalido");
        verificarProblema(mvc.get().uri("/api/v1/disponibilidad/profesionales/15?fecha=03-11-2026")
                .with(usuario()).exchange(), HttpStatus.BAD_REQUEST, "parametro-invalido");
        verificarProblema(mvc.get().uri("/api/v1/disponibilidad/profesionales?fecha=2026-11-03")
                .with(usuario()).exchange(), HttpStatus.BAD_REQUEST, "parametro-invalido");
    }

    @Test
    void unProfesionalInexistenteDevuelve404() {
        when(consultarTurnosLibres.consultar(99, MARTES)).thenThrow(new ProfesionalNoEncontradoException(99));

        MvcTestResult resultado = mvc.get().uri("/api/v1/disponibilidad/profesionales/99?fecha=2026-11-03")
                .with(usuario()).exchange();

        verificarProblema(resultado, HttpStatus.NOT_FOUND, "profesional-no-encontrado");
    }

    @Test
    void siElCatalogoNoRespondeDevuelve503() {
        when(buscarConTurnosLibres.buscar(1, MARTES)).thenThrow(new CatalogoNoDisponibleException());

        MvcTestResult resultado = mvc.get().uri("/api/v1/disponibilidad/profesionales?categoriaId=1&fecha=2026-11-03")
                .with(usuario()).exchange();

        verificarProblema(resultado, HttpStatus.SERVICE_UNAVAILABLE, "catalogo-no-disponible");
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

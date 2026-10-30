package prog2.servicioturnos.usuarios.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import prog2.servicioturnos.usuarios.domain.exception.LoginRepetidoException;
import prog2.servicioturnos.usuarios.domain.model.DatosRegistro;
import prog2.servicioturnos.usuarios.domain.model.TokenAcceso;
import prog2.servicioturnos.usuarios.domain.ports.in.IniciarSesionUseCase;
import prog2.servicioturnos.usuarios.domain.ports.in.RegistrarUsuarioUseCase;
import prog2.servicioturnos.usuarios.infrastructure.web.dto.RegistroRequest;

@WebMvcTest(controllers = CuentaController.class)
class CuentaControllerTests {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private RegistrarUsuarioUseCase registrarUsuario;

    @MockitoBean
    private IniciarSesionUseCase iniciarSesion;

    @Test
    void registraYDevuelve201ConElToken() {
        when(registrarUsuario.registrar(any()))
                .thenReturn(new TokenAcceso("jwt-firmado", Instant.parse("2026-10-30T13:00:00Z")));

        MvcTestResult resultado = registrar("""
                {"login": "ana", "password": "secreta", "firstName": "Ana", "lastName": "Gomez",
                 "email": "ana@mail.com", "langKey": "es"}
                """);

        assertThat(resultado).hasStatus(HttpStatus.CREATED);
        assertThat(resultado).bodyJson().isEqualTo("""
                {"id_token": "jwt-firmado", "venceEn": "2026-10-30T13:00:00Z"}
                """);
        verify(registrarUsuario).registrar(
                new DatosRegistro("ana", "secreta", "Ana", "Gomez", "ana@mail.com", null, "es"));
    }

    @Test
    void sinIdiomaUsaEspaniol() {
        when(registrarUsuario.registrar(any())).thenReturn(new TokenAcceso("jwt", Instant.now()));

        registrar("""
                {"login": "ana", "password": "secreta", "email": "ana@mail.com"}
                """);

        verify(registrarUsuario).registrar(new DatosRegistro("ana", "secreta", null, null, "ana@mail.com", null, "es"));
    }

    @Test
    void datosInvalidosDevuelven400NombrandoCadaCampo() {
        MvcTestResult resultado = registrar("""
                {"login": "con espacio", "password": "123", "email": "no-es-email"}
                """);

        assertThat(resultado).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(resultado).bodyJson().extractingPath("$.type").isEqualTo("urn:turnos:error:datos-invalidos");
        assertThat(resultado).bodyJson().extractingPath("$.detail").asString()
                .contains("login: ", "password: ", "email: ");
        verifyNoInteractions(registrarUsuario);
    }

    @Test
    void unLoginRepetidoDevuelve409() {
        when(registrarUsuario.registrar(any())).thenThrow(new LoginRepetidoException("ana"));

        MvcTestResult resultado = registrar("""
                {"login": "ana", "password": "secreta", "email": "ana@mail.com"}
                """);

        assertThat(resultado).hasStatus(HttpStatus.CONFLICT);
        assertThat(resultado).bodyJson().extractingPath("$.type").isEqualTo("urn:turnos:error:login-repetido");
    }

    @Test
    void laContraseniaNoApareceAlImprimirElPedido() {
        RegistroRequest pedido = new RegistroRequest("ana", "secreta", null, null, "ana@mail.com", null, null);

        assertThat(pedido.toString()).doesNotContain("secreta").contains("****");
    }

    private MvcTestResult registrar(String cuerpo) {
        return mvc.post().uri("/api/v1/cuenta/registro")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo)
                .exchange();
    }
}

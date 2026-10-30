package prog2.servicioturnos.usuarios.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import java.time.Instant;
import java.util.Set;
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
import prog2.servicioturnos.usuarios.domain.exception.CredencialesInvalidasException;
import prog2.servicioturnos.usuarios.domain.exception.CuentaNoEncontradaException;
import prog2.servicioturnos.usuarios.domain.exception.LoginRepetidoException;
import prog2.servicioturnos.usuarios.domain.model.Credenciales;
import prog2.servicioturnos.usuarios.domain.model.DatosRegistro;
import prog2.servicioturnos.usuarios.domain.model.Rol;
import prog2.servicioturnos.usuarios.domain.model.TokenAcceso;
import prog2.servicioturnos.usuarios.domain.model.Usuario;
import prog2.servicioturnos.usuarios.domain.ports.in.ConsultarCuentaUseCase;
import prog2.servicioturnos.usuarios.domain.ports.in.IniciarSesionUseCase;
import prog2.servicioturnos.usuarios.domain.ports.in.RegistrarUsuarioUseCase;
import prog2.servicioturnos.usuarios.infrastructure.web.dto.LoginRequest;
import prog2.servicioturnos.usuarios.infrastructure.web.dto.RegistroRequest;

@WebMvcTest(controllers = CuentaController.class)
@Import({SecurityConfig.class, JwtConfig.class})
class CuentaControllerTests {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private RegistrarUsuarioUseCase registrarUsuario;

    @MockitoBean
    private IniciarSesionUseCase iniciarSesion;

    @MockitoBean
    private ConsultarCuentaUseCase consultarCuenta;

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

    @Test
    void conCredencialesCorrectasDevuelve200ConElToken() {
        when(iniciarSesion.iniciar(new Credenciales("ana", "secreta")))
                .thenReturn(new TokenAcceso("jwt-firmado", Instant.parse("2026-10-30T13:00:00Z")));

        MvcTestResult resultado = iniciar("""
                {"username": "ana", "password": "secreta", "rememberMe": true}
                """);

        assertThat(resultado).hasStatusOk();
        assertThat(resultado).bodyJson().extractingPath("$.id_token").isEqualTo("jwt-firmado");
    }

    @Test
    void conCredencialesIncorrectasDevuelve401() {
        when(iniciarSesion.iniciar(any())).thenThrow(new CredencialesInvalidasException());

        MvcTestResult resultado = iniciar("""
                {"username": "ana", "password": "otra"}
                """);

        assertThat(resultado).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(resultado).bodyJson().extractingPath("$.type")
                .isEqualTo("urn:turnos:error:credenciales-invalidas");
    }

    @Test
    void unLoginSinDatosDevuelve400() {
        MvcTestResult resultado = iniciar("{}");

        assertThat(resultado).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(resultado).bodyJson().extractingPath("$.detail").asString().contains("username", "password");
        verifyNoInteractions(iniciarSesion);
    }

    @Test
    void laContraseniaDelLoginNoApareceAlImprimirElPedido() {
        assertThat(new LoginRequest("ana", "secreta").toString()).doesNotContain("secreta").contains("****");
    }

    @Test
    void laCuentaSeConsultaConElLoginDelToken() {
        Instant creado = Instant.parse("2026-10-30T12:00:00Z");
        when(consultarCuenta.consultar("ana")).thenReturn(new Usuario(1L, "ana", "hash-secreto", "Ana", "Gomez",
                "ana@mail.com", null, true, "es", Set.of(Rol.USUARIO), "ana", creado, "ana", creado));

        MvcTestResult resultado = mvc.get().uri("/api/v1/cuenta").with(usuarioConRol("ROLE_USER")).exchange();

        assertThat(resultado).hasStatusOk();
        assertThat(resultado).bodyJson().isEqualTo("""
                {"id": 1, "login": "ana", "firstName": "Ana", "lastName": "Gomez", "email": "ana@mail.com",
                 "imageUrl": null, "langKey": "es", "activated": true, "authorities": ["ROLE_USER"]}
                """);
        assertThat(resultado).bodyText().doesNotContain("hash-secreto");
    }

    @Test
    void laCuentaSinTokenDevuelve401() {
        MvcTestResult resultado = mvc.get().uri("/api/v1/cuenta").exchange();

        assertThat(resultado).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(resultado).bodyJson().extractingPath("$.type").isEqualTo("urn:turnos:error:no-autenticado");
        verifyNoInteractions(consultarCuenta);
    }

    @Test
    void laCuentaConUnTokenSinElRolDeUsuarioDevuelve403() {
        MvcTestResult resultado = mvc.get().uri("/api/v1/cuenta").with(usuarioConRol("ROLE_OTRO")).exchange();

        assertThat(resultado).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(resultado).bodyJson().extractingPath("$.type").isEqualTo("urn:turnos:error:sin-permiso");
        verifyNoInteractions(consultarCuenta);
    }

    @Test
    void unaCuentaQueYaNoExisteDevuelve404() {
        when(consultarCuenta.consultar("ana")).thenThrow(new CuentaNoEncontradaException("ana"));

        MvcTestResult resultado = mvc.get().uri("/api/v1/cuenta").with(usuarioConRol("ROLE_USER")).exchange();

        assertThat(resultado).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(resultado).bodyJson().extractingPath("$.type").isEqualTo("urn:turnos:error:cuenta-no-encontrada");
    }

    private static RequestPostProcessor usuarioConRol(String rol) {
        return jwt().jwt(token -> token.subject("ana")).authorities(new SimpleGrantedAuthority(rol));
    }

    private MvcTestResult iniciar(String cuerpo) {
        return mvc.post().uri("/api/v1/cuenta/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo)
                .exchange();
    }

    private MvcTestResult registrar(String cuerpo) {
        return mvc.post().uri("/api/v1/cuenta/registro")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo)
                .exchange();
    }
}

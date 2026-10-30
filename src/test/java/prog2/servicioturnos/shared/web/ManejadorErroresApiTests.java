package prog2.servicioturnos.shared.web;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import prog2.servicioturnos.shared.exception.ConflictoException;
import prog2.servicioturnos.shared.exception.NoAutenticadoException;
import prog2.servicioturnos.shared.exception.RecursoNoEncontradoException;

// Prueba solo cómo se traducen los errores; la seguridad tiene sus propias pruebas.
@WebMvcTest(controllers = ManejadorErroresApiTests.ControladorDePrueba.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ManejadorErroresApiTests.ControladorDePrueba.class)
class ManejadorErroresApiTests {

    @Autowired
    private MockMvcTester mvc;

    @Test
    void recursoInexistenteDevuelve404ConSuPropioTipo() {
        MvcTestResult resultado = mvc.get().uri("/prueba/procesos/7").exchange();

        verificarProblema(resultado, HttpStatus.NOT_FOUND, "proceso-no-encontrado", "/prueba/procesos/7");
        assertThat(resultado).bodyJson().extractingPath("$.detail").isEqualTo("No existe un proceso con id 7");
    }

    @Test
    void noAutenticadoDevuelve401ConSuPropioTipo() {
        MvcTestResult resultado = mvc.get().uri("/prueba/sin-autenticar").exchange();

        verificarProblema(resultado, HttpStatus.UNAUTHORIZED, "credenciales-invalidas", "/prueba/sin-autenticar");
    }

    @Test
    void conflictoDevuelve409ConSuPropioTipo() {
        MvcTestResult resultado = mvc.get().uri("/prueba/conflicto").exchange();

        verificarProblema(resultado, HttpStatus.CONFLICT, "login-repetido", "/prueba/conflicto");
    }

    @Test
    void cuerpoConCamposInvalidosNombraCadaCampo() {
        MvcTestResult resultado = mvc.post().uri("/prueba/usuarios")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"login": "", "email": "no-es-un-email"}
                        """)
                .exchange();

        verificarProblema(resultado, HttpStatus.BAD_REQUEST, "datos-invalidos", "/prueba/usuarios");
        assertThat(resultado).bodyJson().extractingPath("$.detail").asString()
                .contains("email: ", "login: ");
    }

    @Test
    void cuerpoQueNoEsJsonDevuelve400SinDetallesDelParser() {
        MvcTestResult resultado = mvc.post().uri("/prueba/usuarios")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{esto no es json")
                .exchange();

        verificarProblema(resultado, HttpStatus.BAD_REQUEST, "parametro-invalido", "/prueba/usuarios");
        assertThat(resultado).bodyJson().extractingPath("$.detail")
                .isEqualTo("El cuerpo del pedido no es un JSON válido");
    }

    @Test
    void parametroConTipoIncorrectoNombraElParametroEnEspanol() {
        MvcTestResult resultado = mvc.get().uri("/prueba/numero?valor=abc").exchange();

        verificarProblema(resultado, HttpStatus.BAD_REQUEST, "parametro-invalido", "/prueba/numero");
        assertThat(resultado).bodyJson().extractingPath("$.detail").isEqualTo("valor: el valor 'abc' no es válido");
        assertThat(resultado).bodyJson().extractingPath("$.title").isEqualTo("Parámetro inválido");
    }

    @Test
    void parametroFueraDeRangoDevuelve400() {
        MvcTestResult resultado = mvc.get().uri("/prueba/pagina?size=500").exchange();

        verificarProblema(resultado, HttpStatus.BAD_REQUEST, "parametro-invalido", "/prueba/pagina");
        assertThat(resultado).bodyJson().extractingPath("$.detail").asString().contains("size");
    }

    @Test
    void rutaInexistenteDevuelve404() {
        MvcTestResult resultado = mvc.get().uri("/no-existe").exchange();

        verificarProblema(resultado, HttpStatus.NOT_FOUND, "ruta-no-encontrada", "/no-existe");
        assertThat(resultado).bodyJson().extractingPath("$.title").isEqualTo("Ruta no encontrada");
    }

    @Test
    void metodoNoPermitidoDevuelve405() {
        MvcTestResult resultado = mvc.delete().uri("/prueba/conflicto").exchange();

        verificarProblema(resultado, HttpStatus.METHOD_NOT_ALLOWED, "metodo-no-permitido", "/prueba/conflicto");
    }

    @Test
    void formatoNoSoportadoDevuelve415() {
        MvcTestResult resultado = mvc.post().uri("/prueba/usuarios")
                .contentType(MediaType.TEXT_PLAIN)
                .content("login=ana")
                .exchange();

        verificarProblema(resultado, HttpStatus.UNSUPPORTED_MEDIA_TYPE, "formato-no-soportado", "/prueba/usuarios");
    }

    @Test
    void errorInesperadoDevuelve500SinDetallesInternos() {
        MvcTestResult resultado = mvc.get().uri("/prueba/falla").exchange();

        verificarProblema(resultado, HttpStatus.INTERNAL_SERVER_ERROR, "error-interno", "/prueba/falla");
        assertThat(resultado).bodyText().doesNotContain("select", "detalle interno");
    }

    private static void verificarProblema(
            MvcTestResult resultado, HttpStatus estado, String codigo, String instancia) {
        assertThat(resultado).hasStatus(estado);
        assertThat(resultado).hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(resultado).bodyJson().extractingPath("$.type")
                .isEqualTo(ManejadorErroresApi.PREFIJO_TIPO + codigo);
        assertThat(resultado).bodyJson().extractingPath("$.status").isEqualTo(estado.value());
        assertThat(resultado).bodyJson().extractingPath("$.instance").isEqualTo(instancia);
        assertThat(resultado).bodyJson().extractingPath("$.title").isNotNull();
        assertThat(resultado).bodyJson().extractingPath("$.detail").isNotNull();
    }

    static class ProcesoNoEncontradoException extends RecursoNoEncontradoException {
        ProcesoNoEncontradoException(long id) {
            super("proceso-no-encontrado", "No existe un proceso con id " + id);
        }
    }

    static class CredencialesDePruebaException extends NoAutenticadoException {
        CredencialesDePruebaException() {
            super("credenciales-invalidas", "Usuario o contraseña incorrectos");
        }
    }

    static class LoginRepetidoException extends ConflictoException {
        LoginRepetidoException() {
            super("login-repetido", "Ya existe una cuenta con ese login");
        }
    }

    record UsuarioDePrueba(@NotBlank String login, @Email String email) {
    }

    // Controlador que existe solo en las pruebas: tira cada error para verificar cómo se traduce.
    @RestController
    static class ControladorDePrueba {

        @GetMapping("/prueba/procesos/{id}")
        String proceso(@PathVariable long id) {
            throw new ProcesoNoEncontradoException(id);
        }

        @GetMapping("/prueba/sin-autenticar")
        String sinAutenticar() {
            throw new CredencialesDePruebaException();
        }

        @GetMapping("/prueba/conflicto")
        String conflicto() {
            throw new LoginRepetidoException();
        }

        @PostMapping("/prueba/usuarios")
        String usuario(@Valid @RequestBody UsuarioDePrueba usuario) {
            return usuario.login();
        }

        @GetMapping("/prueba/numero")
        String numero(@RequestParam int valor) {
            return String.valueOf(valor);
        }

        @GetMapping("/prueba/pagina")
        String pagina(@RequestParam @Max(100) int size) {
            return String.valueOf(size);
        }

        @GetMapping("/prueba/falla")
        String falla() {
            throw new IllegalStateException("detalle interno: select * from usuario");
        }
    }
}

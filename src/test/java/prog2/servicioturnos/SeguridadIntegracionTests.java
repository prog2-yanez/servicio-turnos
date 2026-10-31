package prog2.servicioturnos;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import prog2.servicioturnos.config.JwtProperties;

// Prueba la seguridad con la aplicación completa: tokens reales firmados por turnos y tokens armados a mano.
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SeguridadIntegracionTests {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JwtEncoder codificador;

    @Test
    void conElTokenDelRegistroSeAccedeALaCuenta() {
        Registrado ana = registrar();

        MvcTestResult resultado = cuentaCon("Bearer " + ana.token());

        assertThat(resultado).hasStatusOk();
        assertThat(resultado).bodyJson().extractingPath("$.login").isEqualTo(ana.login());
        assertThat(resultado).bodyText().doesNotContain("contrasenia", "hash");
    }

    @Test
    void sinTokenDevuelve401NoAutenticado() {
        MvcTestResult resultado = mvc.get().uri("/api/v1/cuenta").exchange();

        verificar(resultado, HttpStatus.UNAUTHORIZED, "no-autenticado");
    }

    @Test
    void unTokenAlteradoDevuelve401TokenInvalido() {
        String token = registrar().token();
        String alterado = token.substring(0, token.length() - 4) + "AAAA";

        verificar(cuentaCon("Bearer " + alterado), HttpStatus.UNAUTHORIZED, "token-invalido");
    }

    @Test
    void unTokenVencidoDevuelve401TokenInvalido() {
        Instant hace2Horas = Instant.now().minusSeconds(7200);
        String vencido = firmar(JwtProperties.EMISOR, "ana", "ROLE_USER", hace2Horas, hace2Horas.plusSeconds(3600));

        verificar(cuentaCon("Bearer " + vencido), HttpStatus.UNAUTHORIZED, "token-invalido");
    }

    @Test
    void unTokenDeOtroEmisorDevuelve401TokenInvalido() {
        String ajeno = firmar("otro-servicio", "ana", "ROLE_USER", Instant.now(), Instant.now().plusSeconds(3600));

        verificar(cuentaCon("Bearer " + ajeno), HttpStatus.UNAUTHORIZED, "token-invalido");
    }

    @Test
    void unTokenValidoSinElRolDeUsuarioDevuelve403() {
        Instant ahora = Instant.now();
        String sinRol = firmar(JwtProperties.EMISOR, "ana", "ROLE_OTRO", ahora, ahora.plusSeconds(3600));

        verificar(cuentaCon("Bearer " + sinRol), HttpStatus.FORBIDDEN, "sin-permiso");
    }

    @Test
    void registroLoginYHealthNoPidenToken() {
        Registrado ana = registrar();

        assertThat(mvc.post().uri("/api/v1/cuenta/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\": \"" + ana.login() + "\", \"password\": \"secreta1\"}").exchange())
                .hasStatusOk();
        assertThat(mvc.get().uri("/actuator/health").exchange()).hasStatusOk();
    }

    // Cada prueba registra un login nuevo, así no depende de lo que ya haya en la base.
    private Registrado registrar() {
        String login = "seguridad." + UUID.randomUUID().toString().substring(0, 8);
        String email = login + "@mail.com";
        MvcTestResult resultado = mvc.post().uri("/api/v1/cuenta/registro")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\": \"" + login + "\", \"password\": \"secreta1\", \"email\": \"" + email + "\"}")
                .exchange();
        assertThat(resultado).hasStatus(HttpStatus.CREATED);
        String cuerpo = new String(resultado.getResponse().getContentAsByteArray());
        return new Registrado(login, JsonPath.read(cuerpo, "$.id_token"));
    }

    private MvcTestResult cuentaCon(String autorizacion) {
        return mvc.get().uri("/api/v1/cuenta").header("Authorization", autorizacion).exchange();
    }

    private String firmar(String emisor, String login, String roles, Instant emitido, Instant vence) {
        JwtClaimsSet datos = JwtClaimsSet.builder()
                .issuer(emisor).subject(login).claim("auth", roles).issuedAt(emitido).expiresAt(vence).build();
        JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();
        return codificador.encode(JwtEncoderParameters.from(cabecera, datos)).getTokenValue();
    }

    private static void verificar(MvcTestResult resultado, HttpStatus estado, String codigo) {
        assertThat(resultado).hasStatus(estado);
        assertThat(resultado).bodyJson().extractingPath("$.type").isEqualTo("urn:turnos:error:" + codigo);
    }

    private record Registrado(String login, String token) {
    }
}

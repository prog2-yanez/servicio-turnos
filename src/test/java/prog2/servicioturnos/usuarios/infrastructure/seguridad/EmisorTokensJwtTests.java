package prog2.servicioturnos.usuarios.infrastructure.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import prog2.servicioturnos.config.JwtConfig;
import prog2.servicioturnos.config.JwtProperties;
import prog2.servicioturnos.usuarios.domain.model.Rol;
import prog2.servicioturnos.usuarios.domain.model.TokenAcceso;
import prog2.servicioturnos.usuarios.domain.model.Usuario;

class EmisorTokensJwtTests {

    private static final Instant AHORA = Instant.now();
    private static final JwtProperties PROPIEDADES =
            new JwtProperties("clave-de-pruebas-de-al-menos-32-caracteres", Duration.ofHours(1));

    private final EmisorTokensJwt emisor = new EmisorTokensJwt(
            new JwtConfig().jwtEncoder(PROPIEDADES), PROPIEDADES, Clock.fixed(AHORA, ZoneOffset.UTC));

    @Test
    void elTokenLlevaElLoginElIdLosRolesYVenceEnUnaHora() {
        TokenAcceso token = emisor.emitir(usuario());

        Jwt leido = decodificadorCon(PROPIEDADES.secretoEnBytes()).decode(token.valor());
        assertThat(leido.getSubject()).isEqualTo("ana");
        assertThat(leido.getClaimAsString("iss")).isEqualTo(EmisorTokensJwt.EMISOR);
        assertThat(leido.<Long>getClaim("userId")).isEqualTo(7L);
        assertThat(leido.getClaimAsString("auth")).isEqualTo("ROLE_USER");
        assertThat(token.venceEn()).isEqualTo(AHORA.plus(Duration.ofHours(1)));
    }

    @Test
    void unTokenFirmadoConOtraClaveNoSeAcepta() {
        TokenAcceso token = emisor.emitir(usuario());
        NimbusJwtDecoder otro = decodificadorCon("otra-clave-distinta-de-al-menos-32-caracteres".getBytes());

        assertThatThrownBy(() -> otro.decode(token.valor())).isInstanceOf(JwtException.class);
    }

    private static NimbusJwtDecoder decodificadorCon(byte[] clave) {
        return NimbusJwtDecoder.withSecretKey(new SecretKeySpec(clave, "HmacSHA256")).build();
    }

    private static Usuario usuario() {
        return new Usuario(7L, "ana", "hash", "Ana", "Gomez", "ana@mail.com", null, true, "es",
                Set.of(Rol.USUARIO), "ana", AHORA, "ana", AHORA);
    }
}

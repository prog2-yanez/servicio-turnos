package prog2.servicioturnos.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.time.Clock;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

    // HS256: turnos firma y el catálogo valida con la misma clave (decisión confirmada con el profesor).
    @Bean
    public JwtEncoder jwtEncoder(JwtProperties propiedades) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(clave(propiedades)));
    }

    // Valida firma, vencimiento (exp) y emisor (iss) de cada token que llega.
    @Bean
    public JwtDecoder jwtDecoder(JwtProperties propiedades) {
        NimbusJwtDecoder decodificador = NimbusJwtDecoder.withSecretKey(clave(propiedades))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decodificador.setJwtValidator(JwtValidators.createDefaultWithIssuer(JwtProperties.EMISOR));
        return decodificador;
    }

    @Bean
    public Clock reloj() {
        return Clock.systemUTC();
    }

    private static SecretKeySpec clave(JwtProperties propiedades) {
        return new SecretKeySpec(propiedades.secretoEnBytes(), "HmacSHA256");
    }
}

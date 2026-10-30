package prog2.servicioturnos.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.time.Clock;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

    // HS256: turnos firma y el catálogo valida con la misma clave (decisión confirmada con el profesor).
    @Bean
    public JwtEncoder jwtEncoder(JwtProperties propiedades) {
        SecretKeySpec clave = new SecretKeySpec(propiedades.secretoEnBytes(), "HmacSHA256");
        return new NimbusJwtEncoder(new ImmutableSecret<>(clave));
    }

    @Bean
    public Clock reloj() {
        return Clock.systemUTC();
    }
}

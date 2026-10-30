package prog2.servicioturnos.config;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

// Clave y duración del JWT de los usuarios. Sin una clave de al menos 256 bits el servicio no arranca.
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(String secreto, Duration duracion) {

    static final int LARGO_MINIMO_SECRETO = 32;

    public JwtProperties {
        if (secreto == null || secreto.getBytes(StandardCharsets.UTF_8).length < LARGO_MINIMO_SECRETO) {
            throw new IllegalStateException(
                    "JWT_SECRETO tiene que tener al menos " + LARGO_MINIMO_SECRETO + " caracteres");
        }
    }

    public byte[] secretoEnBytes() {
        return secreto.getBytes(StandardCharsets.UTF_8);
    }
}

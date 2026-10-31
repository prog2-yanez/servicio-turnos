package prog2.servicioturnos.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

// Datos de la cuenta técnica para hablar con la cátedra. Son credenciales: salen del .env y nunca se loguean.
@ConfigurationProperties(prefix = "catedra")
public record CatedraProperties(Api api, String jwt) {

    public record Api(String url, Duration timeoutConexion, Duration timeoutLectura) {
    }
}

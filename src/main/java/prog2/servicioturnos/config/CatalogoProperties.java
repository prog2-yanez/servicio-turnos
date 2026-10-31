package prog2.servicioturnos.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

// Dónde está el servicio de catálogo y cuánto se espera su respuesta.
@ConfigurationProperties(prefix = "catalogo.api")
public record CatalogoProperties(String url, Duration timeoutConexion, Duration timeoutLectura) {
}

package prog2.servicioturnos.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

// Orígenes de navegador que pueden llamar a turnos. Vacía por defecto: la app Android no es un navegador, así que
// no necesita CORS.
@ConfigurationProperties(prefix = "cors")
public record CorsProperties(List<String> origenesPermitidos) {

    public CorsProperties {
        origenesPermitidos = origenesPermitidos == null
                ? List.of()
                : origenesPermitidos.stream().map(String::trim).filter(origen -> !origen.isEmpty()).toList();
    }
}

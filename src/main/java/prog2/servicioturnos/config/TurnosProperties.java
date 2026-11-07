package prog2.servicioturnos.config;

import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;

// Zona horaria de las agendas: los horarios del catálogo son hora local, y con ella se decide qué turno ya pasó.
@ConfigurationProperties(prefix = "turnos")
public record TurnosProperties(ZoneId zonaHoraria) {
}

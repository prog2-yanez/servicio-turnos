package prog2.servicioturnos.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

// Recuperación de procesos pendientes (CU-10).
// - intervalo: cada cuánto corre, contado desde que termina la ronda anterior.
// - margen: tiempo que se le da al evento de la cátedra después de venceEn, antes de cerrarlo.
// - esperaSinHold: para los procesos sin vencimiento conocido (timeout al crear el hold o reinicio antes de
//   tenerlo); es más de lo que dura un hold.
@ConfigurationProperties(prefix = "turnos.recuperacion")
public record RecuperacionProperties(Duration intervalo, Duration margen, Duration esperaSinHold) {
}

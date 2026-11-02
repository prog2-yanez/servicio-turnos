package prog2.servicioturnos.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

// Cuándo la recuperación da por vencido un proceso que no recibió su resultado (CU-10).
// - margen: tiempo que se le da al evento de la cátedra después de venceEn, antes de cerrarlo.
// - esperaSinHold: para los procesos sin vencimiento conocido (timeout al crear el hold o reinicio antes de
//   tenerlo); es más de lo que dura un hold.
@ConfigurationProperties(prefix = "turnos.recuperacion")
public record RecuperacionProperties(Duration margen, Duration esperaSinHold) {
}

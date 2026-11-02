package prog2.servicioturnos.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties({TurnosProperties.class, RecuperacionProperties.class})
public class TurnosConfig {
}

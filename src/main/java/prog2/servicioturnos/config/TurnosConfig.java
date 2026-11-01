package prog2.servicioturnos.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TurnosProperties.class)
public class TurnosConfig {
}

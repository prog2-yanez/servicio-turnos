package prog2.servicioturnos.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(CatedraProperties.class)
public class CatedraConfig {

    // Todos los pedidos a la cátedra van con el JWT técnico; el del usuario nunca sale hacia la cátedra.
    @Bean
    public RestClient catedraRestClient(CatedraProperties propiedades) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(propiedades.api().timeoutConexion());
        fabrica.setReadTimeout(propiedades.api().timeoutLectura());
        return RestClient.builder()
                .baseUrl(propiedades.api().url())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + propiedades.jwt())
                .requestFactory(fabrica)
                .build();
    }
}

package prog2.servicioturnos.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(CatalogoProperties.class)
public class CatalogoConfig {

    // Sin token fijo: en cada pedido se reenvía el JWT del usuario que lo originó (ADR 0003).
    @Bean
    public RestClient catalogoRestClient(CatalogoProperties propiedades) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(propiedades.timeoutConexion());
        fabrica.setReadTimeout(propiedades.timeoutLectura());
        return RestClient.builder()
                .baseUrl(propiedades.url())
                .requestFactory(fabrica)
                .build();
    }
}

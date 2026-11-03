package prog2.servicioturnos.config;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.DefaultCorsProcessor;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import tools.jackson.databind.json.JsonMapper;

// CORS solo para los orígenes de CORS_ORIGENES_PERMITIDOS, con los métodos y headers que usa la API. Spring
// Security usa este filtro por el nombre del bean.
@Configuration
@EnableConfigurationProperties(CorsProperties.class)
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter(CorsProperties propiedades, JsonMapper mapper) {
        CorsConfiguration configuracion = new CorsConfiguration();
        configuracion.setAllowedOrigins(propiedades.origenesPermitidos());
        configuracion.setAllowedMethods(List.of(HttpMethod.GET.name(), HttpMethod.POST.name()));
        configuracion.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE));
        configuracion.setMaxAge(3600L);
        // Se registra para todas las rutas aunque la lista esté vacía; sin configuración Spring no rechaza nada.
        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/**", configuracion);
        CorsFilter filtro = new CorsFilter(fuente);
        filtro.setCorsProcessor(new RechazoComoProblema(mapper));
        return filtro;
    }

    // El filtro corre dentro de la cadena de Spring Security; así no se registra una segunda vez.
    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilterFueraDeLaCadena(CorsFilter corsFilter) {
        FilterRegistrationBean<CorsFilter> registro = new FilterRegistrationBean<>(corsFilter);
        registro.setEnabled(false);
        return registro;
    }

    // Un origen no permitido recibe el mismo ProblemDetail que el resto de los errores, no el texto de Spring.
    static final class RechazoComoProblema extends DefaultCorsProcessor {

        private final JsonMapper mapper;

        RechazoComoProblema(JsonMapper mapper) {
            this.mapper = mapper;
        }

        @Override
        protected void rejectRequest(ServerHttpResponse respuesta) throws IOException {
            respuesta.setStatusCode(HttpStatus.FORBIDDEN);
            respuesta.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
            ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                    HttpStatus.FORBIDDEN, "El origen del pedido no tiene permitido usar este servicio");
            problema.setType(URI.create("urn:turnos:error:origen-no-permitido"));
            problema.setTitle("Origen no permitido");
            respuesta.getBody().write(mapper.writeValueAsBytes(problema));
            respuesta.flush();
        }
    }
}

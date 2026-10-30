package prog2.servicioturnos.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.servlet.HandlerExceptionResolver;

// Todo pide un JWT válido con ROLE_USER, salvo registrarse, iniciar sesión y el health.
@Configuration
public class SecurityConfig {

    static final String CLAIM_ROLES = "auth";

    // Los errores de seguridad pasan por el mismo manejador que el resto, así también salen como ProblemDetail.
    @Bean
    public SecurityFilterChain cadenaDeSeguridad(
            HttpSecurity http, @Qualifier("handlerExceptionResolver") HandlerExceptionResolver manejador)
            throws Exception {
        AuthenticationEntryPoint sinAutenticar =
                (pedido, respuesta, error) -> manejador.resolveException(pedido, respuesta, null, error);
        AccessDeniedHandler sinPermiso =
                (pedido, respuesta, error) -> manejador.resolveException(pedido, respuesta, null, error);
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(pedidos -> pedidos
                        .requestMatchers(HttpMethod.POST, "/api/v1/cuenta/registro", "/api/v1/cuenta/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
                        .anyRequest().hasRole("USER"))
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint(sinAutenticar)
                        .accessDeniedHandler(sinPermiso))
                .oauth2ResourceServer(servidor -> servidor
                        .authenticationEntryPoint(sinAutenticar)
                        .accessDeniedHandler(sinPermiso)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(convertidorDeRoles())));
        return http.build();
    }

    // Los roles viajan en "auth" ya con el prefijo ROLE_, como en JHipster.
    private static JwtAuthenticationConverter convertidorDeRoles() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName(CLAIM_ROLES);
        roles.setAuthorityPrefix("");
        JwtAuthenticationConverter convertidor = new JwtAuthenticationConverter();
        convertidor.setJwtGrantedAuthoritiesConverter(roles);
        return convertidor;
    }
}

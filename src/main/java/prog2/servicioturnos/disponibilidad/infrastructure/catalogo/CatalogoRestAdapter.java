package prog2.servicioturnos.disponibilidad.infrastructure.catalogo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import prog2.servicioturnos.disponibilidad.domain.exception.CatalogoNoDisponibleException;
import prog2.servicioturnos.disponibilidad.domain.exception.ProfesionalDeshabilitadoException;
import prog2.servicioturnos.disponibilidad.domain.exception.ProfesionalNoEncontradoException;
import prog2.servicioturnos.disponibilidad.domain.model.ProfesionalConAgenda;
import prog2.servicioturnos.disponibilidad.domain.model.ProfesionalResumen;
import prog2.servicioturnos.disponibilidad.domain.ports.out.CatalogoClient;
import prog2.servicioturnos.disponibilidad.infrastructure.catalogo.dto.PaginaProfesionalesResponse;
import prog2.servicioturnos.disponibilidad.infrastructure.catalogo.dto.ProfesionalConAgendaResponse;

@Component
public class CatalogoRestAdapter implements CatalogoClient {

    static final String RUTA_PROFESIONAL = "/api/v1/profesionales/{id}";
    static final String RUTA_PROFESIONALES = "/api/v1/profesionales";

    // El máximo que acepta el catálogo por página.
    static final int TAMANIO_PAGINA = 100;

    // Corta el recorrido si el catálogo nunca dijera que es la última página.
    static final int PAGINAS_MAXIMAS = 50;

    private static final Logger log = LoggerFactory.getLogger(CatalogoRestAdapter.class);

    private final RestClient catalogoRestClient;

    public CatalogoRestAdapter(@Qualifier("catalogoRestClient") RestClient catalogoRestClient) {
        this.catalogoRestClient = catalogoRestClient;
    }

    @Override
    public ProfesionalConAgenda obtenerProfesional(long id) {
        ProfesionalConAgendaResponse respuesta;
        try {
            respuesta = catalogoRestClient.get()
                    .uri(RUTA_PROFESIONAL, id)
                    .headers(encabezados -> tokenDelUsuario().ifPresent(encabezados::setBearerAuth))
                    .retrieve()
                    .onStatus(estado -> estado.value() == HttpStatus.NOT_FOUND.value(), (pedido, resp) -> {
                        throw new ProfesionalNoEncontradoException(id);
                    })
                    .onStatus(estado -> estado.value() == HttpStatus.CONFLICT.value(), (pedido, resp) -> {
                        throw new ProfesionalDeshabilitadoException(id);
                    })
                    .onStatus(HttpStatusCode::isError, (pedido, resp) -> {
                        throw noDisponible(resp.getStatusCode());
                    })
                    .body(ProfesionalConAgendaResponse.class);
        } catch (RestClientException e) {
            // Timeout, sin conexión o una respuesta que no se puede leer.
            log.warn("El catálogo no respondió al pedir el profesional {}: {}", id, e.getClass().getSimpleName());
            throw new CatalogoNoDisponibleException();
        }
        if (respuesta == null) {
            log.warn("El catálogo respondió sin cuerpo al pedir el profesional {}", id);
            throw new CatalogoNoDisponibleException();
        }
        return respuesta.aDominio();
    }

    @Override
    public List<ProfesionalResumen> listarHabilitados(long categoriaId) {
        List<ProfesionalResumen> profesionales = new ArrayList<>();
        for (int pagina = 0; pagina < PAGINAS_MAXIMAS; pagina++) {
            PaginaProfesionalesResponse respuesta = pedirPagina(categoriaId, pagina);
            if (respuesta.content() != null) {
                respuesta.content().forEach(profesional -> profesionales.add(profesional.aDominio()));
            }
            if (respuesta.esUltima() || respuesta.content() == null || respuesta.content().isEmpty()) {
                break;
            }
        }
        return profesionales;
    }

    private PaginaProfesionalesResponse pedirPagina(long categoriaId, int pagina) {
        PaginaProfesionalesResponse respuesta;
        try {
            respuesta = catalogoRestClient.get()
                    .uri(uri -> uri.path(RUTA_PROFESIONALES)
                            .queryParam("categoriaId", categoriaId)
                            .queryParam("habilitado", true)
                            .queryParam("page", pagina)
                            .queryParam("size", TAMANIO_PAGINA)
                            .build())
                    .headers(encabezados -> tokenDelUsuario().ifPresent(encabezados::setBearerAuth))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (pedido, resp) -> {
                        throw noDisponible(resp.getStatusCode());
                    })
                    .body(PaginaProfesionalesResponse.class);
        } catch (RestClientException e) {
            log.warn("El catálogo no respondió al listar la categoría {}: {}",
                    categoriaId, e.getClass().getSimpleName());
            throw new CatalogoNoDisponibleException();
        }
        if (respuesta == null) {
            log.warn("El catálogo respondió sin cuerpo al listar la categoría {}", categoriaId);
            throw new CatalogoNoDisponibleException();
        }
        return respuesta;
    }

    // Se reenvía el JWT del usuario que hizo el pedido, así el catálogo sabe quién originó la consulta (ADR 0003).
    private static Optional<String> tokenDelUsuario() {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken autenticacion) {
            return Optional.of(autenticacion.getToken().getTokenValue());
        }
        return Optional.empty();
    }

    private static CatalogoNoDisponibleException noDisponible(HttpStatusCode estado) {
        if (estado.value() == HttpStatus.UNAUTHORIZED.value() || estado.value() == HttpStatus.FORBIDDEN.value()) {
            // El token ya fue válido para turnos: si el catálogo lo rechaza, la configuración entre los dos
            // servicios no coincide (por ejemplo, JWT_SECRETO distinto). No es un error del usuario.
            log.error("El catálogo rechazó el token reenviado (HTTP {}): revisar JWT_SECRETO en los dos servicios",
                    estado.value());
        } else {
            log.warn("El catálogo respondió HTTP {}", estado.value());
        }
        return new CatalogoNoDisponibleException();
    }
}

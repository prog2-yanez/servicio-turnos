package prog2.servicioturnos.disponibilidad.infrastructure.catedra;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import prog2.servicioturnos.disponibilidad.domain.exception.CatedraNoDisponibleException;
import prog2.servicioturnos.disponibilidad.domain.exception.ProfesionalDeshabilitadoException;
import prog2.servicioturnos.disponibilidad.domain.exception.ProfesionalNoEncontradoException;
import prog2.servicioturnos.disponibilidad.domain.model.EstadoOcupacion;
import prog2.servicioturnos.disponibilidad.domain.model.Ocupacion;
import prog2.servicioturnos.disponibilidad.domain.ports.out.OcupacionesCatedra;
import prog2.servicioturnos.disponibilidad.infrastructure.catedra.dto.OcupacionesResponse;
import prog2.servicioturnos.shared.catedra.LectorErrorCatedra;

@Component
public class OcupacionesCatedraRestAdapter implements OcupacionesCatedra {

    static final String RUTA_OCUPACIONES = "/api/appointment-occupancies";

    private static final Logger log = LoggerFactory.getLogger(OcupacionesCatedraRestAdapter.class);

    private final RestClient catedraRestClient;
    private final LectorErrorCatedra lectorError;

    public OcupacionesCatedraRestAdapter(
            @Qualifier("catedraRestClient") RestClient catedraRestClient, LectorErrorCatedra lectorError) {
        this.catedraRestClient = catedraRestClient;
        this.lectorError = lectorError;
    }

    @Override
    public List<Ocupacion> obtener(long profesionalId, LocalDate fecha) {
        OcupacionesResponse respuesta;
        try {
            respuesta = catedraRestClient.get()
                    .uri(uri -> uri.path(RUTA_OCUPACIONES)
                            .queryParam("professionalId", profesionalId)
                            .queryParam("from", fecha)
                            .queryParam("to", fecha)
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (pedido, resp) -> {
                        throw traducirError(resp, profesionalId);
                    })
                    .body(OcupacionesResponse.class);
        } catch (RestClientException | IllegalArgumentException e) {
            // Timeout, sin conexión o una respuesta ilegible. IllegalArgumentException aparece si falta la URL.
            // No se encadena la excepción original porque su mensaje puede incluir la URL de la cátedra.
            log.warn("La cátedra no respondió las ocupaciones: {}", e.getClass().getSimpleName());
            throw new CatedraNoDisponibleException();
        }
        if (respuesta == null || respuesta.occupancies() == null) {
            return List.of();
        }
        return sinRepetidas(respuesta.occupancies());
    }

    private RuntimeException traducirError(ClientHttpResponse respuesta, long profesionalId) throws IOException {
        int estado = respuesta.getStatusCode().value();
        String codigo = lectorError.codigo(respuesta).orElse("sin code");
        if (estado == 404 && "PROFESSIONAL_NOT_FOUND".equals(codigo)) {
            return new ProfesionalNoEncontradoException(profesionalId);
        }
        if (estado == 400 && "PROFESSIONAL_DISABLED".equals(codigo)) {
            return new ProfesionalDeshabilitadoException(profesionalId);
        }
        if (LectorErrorCatedra.esProblemaDeCuentaTecnica(estado, codigo)) {
            // Es un problema de la cuenta técnica, no del usuario: el JWT técnico venció o no corresponde.
            log.error("La cátedra rechazó la cuenta técnica (HTTP {}, {}): revisar CATEDRA_JWT", estado, codigo);
        } else {
            log.warn("La cátedra respondió HTTP {} ({}) al pedir ocupaciones", estado, codigo);
        }
        return new CatedraNoDisponibleException();
    }

    // Si un turno aparece más de una vez, prevalece CONFIRMED sobre HELD (contrato, sección 8).
    private static List<Ocupacion> sinRepetidas(List<OcupacionesResponse.Ocupacion> recibidas) {
        Map<String, Ocupacion> porTurno = new LinkedHashMap<>();
        for (OcupacionesResponse.Ocupacion recibida : recibidas) {
            Optional<EstadoOcupacion> estado = estado(recibida.status());
            if (estado.isEmpty() || recibida.date() == null || recibida.startTime() == null) {
                log.warn("Se ignora una ocupación incompleta o con estado desconocido ({})", recibida.status());
                continue;
            }
            Ocupacion ocupacion = new Ocupacion(recibida.date(), recibida.startTime(), estado.get(),
                    recibida.expiresAt());
            porTurno.merge(clave(recibida.date(), recibida.startTime()), ocupacion,
                    (anterior, nueva) -> anterior.estado() == EstadoOcupacion.CONFIRMADA ? anterior : nueva);
        }
        List<Ocupacion> resultado = new ArrayList<>(porTurno.values());
        resultado.sort(Comparator.comparing(Ocupacion::fecha).thenComparing(Ocupacion::horaInicio));
        return resultado;
    }

    private static Optional<EstadoOcupacion> estado(String status) {
        if ("CONFIRMED".equals(status)) {
            return Optional.of(EstadoOcupacion.CONFIRMADA);
        }
        if ("HELD".equals(status)) {
            return Optional.of(EstadoOcupacion.RETENIDA);
        }
        return Optional.empty();
    }

    private static String clave(LocalDate fecha, LocalTime hora) {
        return fecha + "T" + hora;
    }
}

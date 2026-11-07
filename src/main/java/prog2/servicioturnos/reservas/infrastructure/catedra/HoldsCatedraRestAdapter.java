package prog2.servicioturnos.reservas.infrastructure.catedra;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import prog2.servicioturnos.disponibilidad.domain.exception.CatedraNoDisponibleException;
import prog2.servicioturnos.reservas.domain.exception.ProcesoVencidoException;
import prog2.servicioturnos.reservas.domain.exception.ReservaRechazadaException;
import prog2.servicioturnos.reservas.domain.exception.ResultadoInciertoException;
import prog2.servicioturnos.reservas.domain.exception.TurnoNoDisponibleException;
import prog2.servicioturnos.reservas.domain.exception.TurnoOcupadoException;
import prog2.servicioturnos.reservas.domain.model.HoldCreado;
import prog2.servicioturnos.reservas.domain.ports.out.HoldsCatedra;
import prog2.servicioturnos.reservas.infrastructure.catedra.dto.ConfirmarHoldRequest;
import prog2.servicioturnos.reservas.infrastructure.catedra.dto.CrearHoldRequest;
import prog2.servicioturnos.reservas.infrastructure.catedra.dto.HoldResponse;
import prog2.servicioturnos.shared.catedra.LectorErrorCatedra;

@Component
public class HoldsCatedraRestAdapter implements HoldsCatedra {

    static final String RUTA_HOLDS = "/api/appointment-holds";
    static final String RUTA_CONFIRMAR = "/api/appointment-holds/{holdId}/confirm";

    private static final Logger log = LoggerFactory.getLogger(HoldsCatedraRestAdapter.class);

    private final RestClient catedraRestClient;
    private final LectorErrorCatedra lectorError;

    public HoldsCatedraRestAdapter(
            @Qualifier("catedraRestClient") RestClient catedraRestClient, LectorErrorCatedra lectorError) {
        this.catedraRestClient = catedraRestClient;
        this.lectorError = lectorError;
    }

    @Override
    public HoldCreado crearHold(long profesionalId, LocalDate fecha, LocalTime horaInicio, String externalPatientId) {
        HoldResponse respuesta = enviar("crear el hold", catedraRestClient.post()
                .uri(RUTA_HOLDS)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CrearHoldRequest(profesionalId, fecha, horaInicio, externalPatientId)));
        if (respuesta.holdId() == null || respuesta.reservationProcessId() == null || respuesta.expiresAt() == null) {
            throw new ResultadoInciertoException("la cátedra creó el hold sin todos sus datos");
        }
        return new HoldCreado(respuesta.holdId(), respuesta.reservationProcessId(), respuesta.expiresAt());
    }

    @Override
    public Instant confirmar(String holdId, String externalPatientId, String nombre, String apellido) {
        HoldResponse respuesta = enviar("confirmar el hold", catedraRestClient.post()
                .uri(RUTA_CONFIRMAR, holdId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ConfirmarHoldRequest(externalPatientId, nombre, apellido)));
        if (respuesta.expiresAt() == null) {
            throw new ResultadoInciertoException("la cátedra confirmó sin informar el vencimiento");
        }
        return respuesta.expiresAt();
    }

    private HoldResponse enviar(String accion, RestClient.RequestBodySpec pedido) {
        HoldResponse respuesta;
        try {
            respuesta = pedido.retrieve()
                    .onStatus(HttpStatusCode::isError, (req, resp) -> {
                        throw traducirError(accion, resp);
                    })
                    .body(HoldResponse.class);
        } catch (RestClientException | IllegalArgumentException e) {
            // Un timeout no prueba que falló: la cátedra pudo haberlo hecho. No se encadena la excepción original
            // porque su mensaje puede incluir la URL de la cátedra.
            log.warn("La cátedra no respondió al {}: {}", accion, e.getClass().getSimpleName());
            throw new ResultadoInciertoException("sin respuesta al " + accion);
        }
        if (respuesta == null) {
            throw new ResultadoInciertoException("respuesta vacía al " + accion);
        }
        return respuesta;
    }

    private RuntimeException traducirError(String accion, ClientHttpResponse respuesta) throws IOException {
        int estado = respuesta.getStatusCode().value();
        String codigo = lectorError.codigo(respuesta).orElse("sin code");
        if (estado >= 500) {
            log.warn("La cátedra respondió HTTP {} al {}", estado, accion);
            return new ResultadoInciertoException("HTTP " + estado + " al " + accion);
        }
        if (LectorErrorCatedra.esProblemaDeCuentaTecnica(estado, codigo)) {
            log.error("La cátedra rechazó la cuenta técnica al {} (HTTP {}, {}): revisar CATEDRA_JWT",
                    accion, estado, codigo);
            return new CatedraNoDisponibleException();
        }
        log.info("La cátedra rechazó {} (HTTP {}, {})", accion, estado, codigo);
        return switch (codigo) {
            case "SLOT_ALREADY_HELD", "SLOT_ALREADY_RESERVED" -> new TurnoOcupadoException();
            case "INVALID_SLOT", "PROFESSIONAL_NOT_FOUND", "PROFESSIONAL_DISABLED" -> new TurnoNoDisponibleException();
            case "HOLD_EXPIRED" -> new ProcesoVencidoException();
            default -> new ReservaRechazadaException();
        };
    }
}

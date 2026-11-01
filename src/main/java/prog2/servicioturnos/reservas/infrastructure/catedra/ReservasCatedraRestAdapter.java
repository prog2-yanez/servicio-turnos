package prog2.servicioturnos.reservas.infrastructure.catedra;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import prog2.servicioturnos.disponibilidad.domain.exception.CatedraNoDisponibleException;
import prog2.servicioturnos.reservas.domain.model.EstadoReservaCatedra;
import prog2.servicioturnos.reservas.domain.model.ReservaCatedra;
import prog2.servicioturnos.reservas.domain.ports.out.ReservasCatedra;
import prog2.servicioturnos.reservas.infrastructure.catedra.dto.ReservaCatedraResponse;
import prog2.servicioturnos.shared.catedra.LectorErrorCatedra;

@Component
public class ReservasCatedraRestAdapter implements ReservasCatedra {

    static final String RUTA_RESERVAS = "/api/appointments";
    static final int TAMANIO_PAGINA = 100;

    // Corta el recorrido si la cátedra nunca devolviera una página incompleta.
    static final int PAGINAS_MAXIMAS = 50;

    private static final ParameterizedTypeReference<List<ReservaCatedraResponse>> LISTA =
            new ParameterizedTypeReference<>() {
            };

    private static final Logger log = LoggerFactory.getLogger(ReservasCatedraRestAdapter.class);

    private final RestClient catedraRestClient;
    private final LectorErrorCatedra lectorError;

    public ReservasCatedraRestAdapter(
            @Qualifier("catedraRestClient") RestClient catedraRestClient, LectorErrorCatedra lectorError) {
        this.catedraRestClient = catedraRestClient;
        this.lectorError = lectorError;
    }

    @Override
    public List<ReservaCatedra> deUnPaciente(String externalPatientId) {
        List<ReservaCatedra> reservas = new ArrayList<>();
        int ajenas = 0;
        for (int pagina = 0; pagina < PAGINAS_MAXIMAS; pagina++) {
            List<ReservaCatedraResponse> recibidas = pedirPagina(externalPatientId, pagina);
            for (ReservaCatedraResponse recibida : recibidas) {
                // La cátedra devuelve reservas de toda la cuenta técnica: aunque se pidió filtrado, se vuelve a
                // controlar para que nunca se muestre una reserva de otro usuario.
                if (!externalPatientId.equals(recibida.externalPatientId())) {
                    ajenas++;
                    continue;
                }
                aDominio(recibida).ifPresent(reservas::add);
            }
            if (recibidas.size() < TAMANIO_PAGINA) {
                break;
            }
        }
        if (ajenas > 0) {
            log.warn("La cátedra devolvió {} reservas de otros pacientes; se descartaron", ajenas);
        }
        return reservas;
    }

    private List<ReservaCatedraResponse> pedirPagina(String externalPatientId, int pagina) {
        List<ReservaCatedraResponse> respuesta;
        try {
            respuesta = catedraRestClient.get()
                    .uri(uri -> uri.path(RUTA_RESERVAS)
                            .queryParam("externalPatientId", externalPatientId)
                            .queryParam("page", pagina)
                            .queryParam("size", TAMANIO_PAGINA)
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (pedido, resp) -> {
                        throw traducirError(resp);
                    })
                    .body(LISTA);
        } catch (RestClientException | IllegalArgumentException e) {
            // No se encadena la excepción original porque su mensaje puede incluir la URL de la cátedra.
            log.warn("La cátedra no respondió las reservas: {}", e.getClass().getSimpleName());
            throw new CatedraNoDisponibleException();
        }
        return respuesta == null ? List.of() : respuesta;
    }

    private CatedraNoDisponibleException traducirError(ClientHttpResponse respuesta) throws IOException {
        int estado = respuesta.getStatusCode().value();
        String codigo = lectorError.codigo(respuesta).orElse("sin code");
        if (LectorErrorCatedra.esProblemaDeCuentaTecnica(estado, codigo)) {
            log.error("La cátedra rechazó la cuenta técnica al listar reservas (HTTP {}, {}): revisar CATEDRA_JWT",
                    estado, codigo);
        } else {
            log.warn("La cátedra respondió HTTP {} ({}) al listar reservas", estado, codigo);
        }
        return new CatedraNoDisponibleException();
    }

    private static Optional<ReservaCatedra> aDominio(ReservaCatedraResponse recibida) {
        Optional<EstadoReservaCatedra> estado = estado(recibida.status());
        if (estado.isEmpty() || recibida.appointmentDate() == null || recibida.startTime() == null
                || recibida.professionalId() == null) {
            log.warn("Se ignora una reserva incompleta o con estado desconocido ({})", recibida.status());
            return Optional.empty();
        }
        return Optional.of(new ReservaCatedra(recibida.reservationId(), recibida.reservationProcessId(),
                recibida.externalPatientId(), recibida.appointmentDate(), recibida.startTime(), recibida.endTime(),
                recibida.professionalId(), recibida.professionalFirstName(), recibida.professionalLastName(),
                estado.get(), recibida.confirmedAt(), recibida.cancelledAt(), recibida.cancellationReason()));
    }

    private static Optional<EstadoReservaCatedra> estado(String status) {
        return Optional.ofNullable(switch (String.valueOf(status)) {
            case "PHONE_PENDING" -> EstadoReservaCatedra.TELEFONO_PENDIENTE;
            case "CONFIRMED" -> EstadoReservaCatedra.CONFIRMADA;
            case "CANCELLED" -> EstadoReservaCatedra.CANCELADA;
            case "FAILED" -> EstadoReservaCatedra.FALLIDA;
            default -> null;
        });
    }
}

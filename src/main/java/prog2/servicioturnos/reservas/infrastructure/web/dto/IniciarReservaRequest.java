package prog2.servicioturnos.reservas.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import prog2.servicioturnos.reservas.domain.model.PedidoReserva;

public record IniciarReservaRequest(
        @NotNull(message = "es obligatorio") Long profesionalId,
        @NotNull(message = "es obligatoria") LocalDate fecha,
        @NotNull(message = "es obligatoria") @JsonFormat(pattern = "HH:mm") LocalTime horaInicio) {

    public PedidoReserva aPedido() {
        return new PedidoReserva(profesionalId, fecha, horaInicio);
    }
}

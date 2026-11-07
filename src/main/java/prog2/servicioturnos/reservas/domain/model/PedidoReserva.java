package prog2.servicioturnos.reservas.domain.model;

import java.time.LocalDate;
import java.time.LocalTime;

// El turno libre que eligió el usuario.
public record PedidoReserva(long profesionalId, LocalDate fecha, LocalTime horaInicio) {
}

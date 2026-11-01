package prog2.servicioturnos.reservas.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import prog2.servicioturnos.reservas.domain.model.EstadoProceso;
import prog2.servicioturnos.reservas.domain.model.ReservaPropia;
import prog2.servicioturnos.reservas.domain.model.ReservasPropias;

// Las reservas del usuario. desactualizado = true avisa que la cátedra no respondió y la lista puede no estar
// al día.
public record ReservasPropiasResponse(List<Reserva> reservas, boolean desactualizado) {

    public static ReservasPropiasResponse desde(ReservasPropias propias) {
        return new ReservasPropiasResponse(propias.reservas().stream().map(Reserva::desde).toList(),
                propias.desactualizado());
    }

    public record Reserva(
            Long procesoId,
            EstadoProceso estado,
            LocalDate fecha,
            @JsonFormat(pattern = "HH:mm") LocalTime horaInicio,
            @JsonFormat(pattern = "HH:mm") LocalTime horaFin,
            Profesional profesional,
            Instant confirmadaEn,
            Instant canceladaEn,
            String motivoCancelacion) {

        static Reserva desde(ReservaPropia reserva) {
            return new Reserva(reserva.procesoId(), reserva.estado(), reserva.fecha(), reserva.horaInicio(),
                    reserva.horaFin(), new Profesional(reserva.profesionalId(), reserva.profesionalNombre(),
                    reserva.profesionalApellido()), reserva.confirmadaEn(), reserva.canceladaEn(),
                    reserva.motivoCancelacion());
        }
    }

    public record Profesional(long id, String nombre, String apellido) {
    }
}

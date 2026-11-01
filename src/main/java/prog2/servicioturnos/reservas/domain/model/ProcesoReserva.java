package prog2.servicioturnos.reservas.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

// Un intento de reservar un turno, a nombre del usuario que lo inició. Cada cambio devuelve un proceso nuevo y
// controla que la transición sea válida: un proceso que ya terminó no se reabre.
public record ProcesoReserva(
        Long id,
        long usuarioId,
        long profesionalId,
        LocalDate fecha,
        LocalTime horaInicio,
        long versionCatalogo,
        EstadoProceso estado,
        String holdId,
        String reservationProcessId,
        Instant venceEn,
        Instant creadoEn,
        Instant actualizadoEn) {

    public static ProcesoReserva iniciar(long usuarioId, long profesionalId, LocalDate fecha, LocalTime horaInicio,
            long versionCatalogo, Instant ahora) {
        return new ProcesoReserva(null, usuarioId, profesionalId, fecha, horaInicio, versionCatalogo,
                EstadoProceso.INICIADO, null, null, null, ahora, ahora);
    }

    // La cátedra creó el hold: se guardan sus identificadores y cuándo vence. Sigue INICIADO hasta confirmarlo.
    public ProcesoReserva conHold(String nuevoHoldId, String nuevoReservationProcessId, Instant nuevoVenceEn,
            Instant ahora) {
        exigirEstado(EstadoProceso.INICIADO);
        return new ProcesoReserva(id, usuarioId, profesionalId, fecha, horaInicio, versionCatalogo, estado,
                nuevoHoldId, nuevoReservationProcessId, nuevoVenceEn, creadoEn, ahora);
    }

    // La cátedra aceptó la confirmación inicial. Su expiresAt manda: guardarlo no extiende el hold.
    public ProcesoReserva esperandoTelefono(Instant nuevoVenceEn, Instant ahora) {
        exigirEstado(EstadoProceso.INICIADO);
        if (holdId == null) {
            throw new IllegalStateException("No se puede confirmar un proceso sin hold");
        }
        return new ProcesoReserva(id, usuarioId, profesionalId, fecha, horaInicio, versionCatalogo,
                EstadoProceso.ESPERANDO_TELEFONO, holdId, reservationProcessId, nuevoVenceEn, creadoEn, ahora);
    }

    public ProcesoReserva rechazado(Instant ahora) {
        return terminar(EstadoProceso.RECHAZADO, ahora);
    }

    public ProcesoReserva vencido(Instant ahora) {
        return terminar(EstadoProceso.VENCIDO, ahora);
    }

    // No se sabe si la cátedra hizo el cambio: no se toma como rechazo ni se reintenta a ciegas.
    public ProcesoReserva incierto(Instant ahora) {
        exigirAbierto();
        return conEstado(EstadoProceso.INCIERTO, ahora);
    }

    private ProcesoReserva terminar(EstadoProceso estadoFinal, Instant ahora) {
        exigirAbierto();
        return conEstado(estadoFinal, ahora);
    }

    private ProcesoReserva conEstado(EstadoProceso nuevoEstado, Instant ahora) {
        return new ProcesoReserva(id, usuarioId, profesionalId, fecha, horaInicio, versionCatalogo, nuevoEstado,
                holdId, reservationProcessId, venceEn, creadoEn, ahora);
    }

    private void exigirEstado(EstadoProceso esperado) {
        if (estado != esperado) {
            throw new IllegalStateException("El proceso está " + estado + " y se esperaba " + esperado);
        }
    }

    private void exigirAbierto() {
        if (estado.esFinal()) {
            throw new IllegalStateException("El proceso ya terminó como " + estado + " y no se reabre");
        }
    }
}

package prog2.servicioturnos.reservas.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

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
        String requestEventId,
        Instant venceEn,
        Instant creadoEn,
        Instant actualizadoEn) {

    // Puede llegar el pedido de teléfono aunque la confirmación todavía no se haya guardado (INICIADO con hold) o
    // haya quedado INCIERTA: el pedido demuestra que la cátedra sí la recibió.
    private static final Set<EstadoProceso> PUEDEN_RECIBIR_PEDIDO_DE_TELEFONO =
            Set.of(EstadoProceso.INICIADO, EstadoProceso.INCIERTO, EstadoProceso.ESPERANDO_TELEFONO);

    // Después de un rechazo el usuario puede corregir y volver a mandar el teléfono.
    private static final Set<EstadoProceso> PUEDEN_ENVIAR_TELEFONO =
            Set.of(EstadoProceso.ESPERANDO_TELEFONO, EstadoProceso.TELEFONO_RECHAZADO);

    public static ProcesoReserva iniciar(long usuarioId, long profesionalId, LocalDate fecha, LocalTime horaInicio,
            long versionCatalogo, Instant ahora) {
        return new ProcesoReserva(null, usuarioId, profesionalId, fecha, horaInicio, versionCatalogo,
                EstadoProceso.INICIADO, null, null, null, null, ahora, ahora);
    }

    // La cátedra creó el hold: se guardan sus identificadores y cuándo vence. Sigue INICIADO hasta confirmarlo.
    public ProcesoReserva conHold(String nuevoHoldId, String nuevoReservationProcessId, Instant nuevoVenceEn,
            Instant ahora) {
        exigirEstado(EstadoProceso.INICIADO);
        return new ProcesoReserva(id, usuarioId, profesionalId, fecha, horaInicio, versionCatalogo, estado,
                nuevoHoldId, nuevoReservationProcessId, requestEventId, nuevoVenceEn, creadoEn, ahora);
    }

    // La cátedra aceptó la confirmación inicial. Su expiresAt manda: guardarlo no extiende el hold.
    public ProcesoReserva esperandoTelefono(Instant nuevoVenceEn, Instant ahora) {
        exigirEstado(EstadoProceso.INICIADO);
        exigirHold();
        return new ProcesoReserva(id, usuarioId, profesionalId, fecha, horaInicio, versionCatalogo,
                EstadoProceso.ESPERANDO_TELEFONO, holdId, reservationProcessId, requestEventId, nuevoVenceEn,
                creadoEn, ahora);
    }

    // Llegó AdditionalInformationRequested: se guarda su eventId para devolverlo con el teléfono.
    public ProcesoReserva telefonoPedido(String nuevoRequestEventId, Instant nuevoVenceEn, Instant ahora) {
        if (!PUEDEN_RECIBIR_PEDIDO_DE_TELEFONO.contains(estado)) {
            throw new IllegalStateException("El proceso está " + estado + " y no espera un pedido de teléfono");
        }
        exigirHold();
        return new ProcesoReserva(id, usuarioId, profesionalId, fecha, horaInicio, versionCatalogo,
                EstadoProceso.ESPERANDO_TELEFONO, holdId, reservationProcessId, nuevoRequestEventId,
                vencimientoInformado(nuevoVenceEn), creadoEn, ahora);
    }

    // Llegó AdditionalInformationRejected: el usuario puede corregir el teléfono hasta el nuevo vencimiento.
    public ProcesoReserva telefonoRechazado(Instant nuevoVenceEn, Instant ahora) {
        exigirEstado(EstadoProceso.TELEFONO_ENVIADO);
        return new ProcesoReserva(id, usuarioId, profesionalId, fecha, horaInicio, versionCatalogo,
                EstadoProceso.TELEFONO_RECHAZADO, holdId, reservationProcessId, requestEventId,
                vencimientoInformado(nuevoVenceEn), creadoEn, ahora);
    }

    // La cátedra pidió el teléfono y todavía no se mandó uno que esté esperando respuesta.
    public boolean puedeEnviarTelefono() {
        return PUEDEN_ENVIAR_TELEFONO.contains(estado) && requestEventId != null;
    }

    public boolean vencioAntesDe(Instant ahora) {
        return venceEn != null && !venceEn.isAfter(ahora);
    }

    public ProcesoReserva telefonoEnviado(Instant ahora) {
        if (!puedeEnviarTelefono()) {
            throw new IllegalStateException("El proceso está " + estado + " y no espera un teléfono");
        }
        return conEstado(EstadoProceso.TELEFONO_ENVIADO, ahora);
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
                holdId, reservationProcessId, requestEventId, venceEn, creadoEn, ahora);
    }

    // Si un evento no trae vencimiento, se conserva el que ya se conocía.
    private Instant vencimientoInformado(Instant nuevoVenceEn) {
        return nuevoVenceEn != null ? nuevoVenceEn : venceEn;
    }

    private void exigirEstado(EstadoProceso esperado) {
        if (estado != esperado) {
            throw new IllegalStateException("El proceso está " + estado + " y se esperaba " + esperado);
        }
    }

    private void exigirHold() {
        if (holdId == null) {
            throw new IllegalStateException("El proceso todavía no tiene hold");
        }
    }

    private void exigirAbierto() {
        if (estado.esFinal()) {
            throw new IllegalStateException("El proceso ya terminó como " + estado + " y no se reabre");
        }
    }
}

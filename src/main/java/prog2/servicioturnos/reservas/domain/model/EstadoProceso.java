package prog2.servicioturnos.reservas.domain.model;

// Estados de un proceso de reserva (documentación, casos de uso de turnos). Un estado final no cambia más.
public enum EstadoProceso {

    // Se registró el proceso y se está pidiendo el hold.
    INICIADO(false),

    // La cátedra aceptó la confirmación inicial y espera el teléfono.
    ESPERANDO_TELEFONO(false),

    // Se publicó el teléfono y se espera el resultado.
    TELEFONO_ENVIADO(false),

    // La cátedra rechazó el teléfono; se puede corregir hasta que venza.
    TELEFONO_RECHAZADO(false),

    // Hubo un timeout y no se sabe qué hizo la cátedra; lo resuelve la recuperación.
    INCIERTO(false),

    CONFIRMADO(true),
    VENCIDO(true),
    INVALIDO(true),
    CANCELADO(true),

    // La cátedra rechazó el hold: turno ocupado u horario inválido.
    RECHAZADO(true);

    private final boolean esFinal;

    EstadoProceso(boolean esFinal) {
        this.esFinal = esFinal;
    }

    public boolean esFinal() {
        return esFinal;
    }
}

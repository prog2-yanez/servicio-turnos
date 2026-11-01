package prog2.servicioturnos.reservas.domain.model;

// Estado de una reserva según la cátedra (contrato, sección 11).
public enum EstadoReservaCatedra {
    TELEFONO_PENDIENTE,
    CONFIRMADA,
    CANCELADA,
    FALLIDA
}

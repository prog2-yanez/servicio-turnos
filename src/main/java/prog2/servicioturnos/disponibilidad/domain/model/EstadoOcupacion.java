package prog2.servicioturnos.disponibilidad.domain.model;

public enum EstadoOcupacion {

    // El turno ya está reservado.
    CONFIRMADA,

    // Alguien lo está reservando: está retenido hasta venceEn.
    RETENIDA
}

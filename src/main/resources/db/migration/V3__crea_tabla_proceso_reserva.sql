-- Procesos de reserva de turnos. Cada uno pertenece al usuario que lo inició; la cátedra solo conoce la
-- cuenta técnica, así que la propiedad la controla turnos.

CREATE TABLE proceso_reserva (
    id                     BIGSERIAL    PRIMARY KEY,
    usuario_id             BIGINT       NOT NULL REFERENCES usuario (id),
    profesional_id         BIGINT       NOT NULL,
    fecha                  DATE         NOT NULL,
    hora_inicio            TIME         NOT NULL,
    version_catalogo       BIGINT       NOT NULL,
    estado                 VARCHAR(20)  NOT NULL
        CHECK (estado IN ('INICIADO', 'ESPERANDO_TELEFONO', 'TELEFONO_ENVIADO', 'TELEFONO_RECHAZADO', 'INCIERTO',
                          'CONFIRMADO', 'VENCIDO', 'INVALIDO', 'CANCELADO', 'RECHAZADO')),
    hold_id                VARCHAR(100),
    reservation_process_id VARCHAR(100) UNIQUE,
    vence_en               TIMESTAMPTZ,
    creado_en              TIMESTAMPTZ  NOT NULL,
    actualizado_en         TIMESTAMPTZ  NOT NULL
);

-- Un solo proceso abierto por usuario (decisión confirmada con el profesor). El índice lo garantiza aunque
-- lleguen dos pedidos a la vez: la base deja guardar uno solo.
CREATE UNIQUE INDEX ux_proceso_abierto_por_usuario ON proceso_reserva (usuario_id)
    WHERE estado IN ('INICIADO', 'ESPERANDO_TELEFONO', 'TELEFONO_ENVIADO', 'TELEFONO_RECHAZADO', 'INCIERTO');

CREATE INDEX idx_proceso_reserva_usuario ON proceso_reserva (usuario_id);

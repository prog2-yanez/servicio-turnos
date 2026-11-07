-- Eventos de Kafka de la cátedra que ya se procesaron. Kafka puede entregar el mismo evento más de una vez;
-- si su eventId está acá, se descarta sin efectos.
CREATE TABLE evento_procesado (
    event_id     VARCHAR(100) PRIMARY KEY,
    tipo         VARCHAR(100) NOT NULL,
    procesado_en TIMESTAMPTZ  NOT NULL
);

-- Resultado final del proceso: el id de la reserva en la cátedra (si se confirmó) y el motivo que informó la
-- cátedra al invalidar o cancelar.
ALTER TABLE proceso_reserva ADD COLUMN reservation_id VARCHAR(100);
ALTER TABLE proceso_reserva ADD COLUMN motivo VARCHAR(500);

-- eventId del AdditionalInformationRequested de la cátedra. Se devuelve como requestEventId al mandar el teléfono.
ALTER TABLE proceso_reserva ADD COLUMN request_event_id VARCHAR(100);

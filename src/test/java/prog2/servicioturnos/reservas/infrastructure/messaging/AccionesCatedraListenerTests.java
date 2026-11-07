package prog2.servicioturnos.reservas.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import prog2.servicioturnos.reservas.domain.model.EventoCatedra;
import prog2.servicioturnos.reservas.domain.ports.in.ProcesarAccionesCatedraUseCase;
import tools.jackson.databind.json.JsonMapper;

class AccionesCatedraListenerTests {

    private final ProcesarAccionesCatedraUseCase procesar = mock(ProcesarAccionesCatedraUseCase.class);
    private final AccionesCatedraListener listener =
            new AccionesCatedraListener(procesar, JsonMapper.builder().build());

    @Test
    void elPedidoDeTelefonoLlegaAlCasoDeUsoIgnorandoCamposDesconocidos() {
        listener.recibir("""
                {"eventId": "ev-1", "eventType": "AdditionalInformationRequested", "reservationProcessId": "rp-9",
                 "holdId": 77, "requiredField": "PHONE_NUMBER", "message": "Ingresá tu teléfono",
                 "expiresAt": "2026-11-02T12:10:00Z", "occurredAt": "2026-11-02T12:00:01Z", "campoNuevo": 1}
                """);

        verify(procesar).telefonoPedido(new EventoCatedra("ev-1", "AdditionalInformationRequested", "rp-9",
                Instant.parse("2026-11-02T12:10:00Z"), null, null));
    }

    @Test
    void elRechazoDelTelefonoLlegaAlCasoDeUso() {
        listener.recibir("""
                {"eventId": "ev-2", "eventType": "AdditionalInformationRejected", "reservationProcessId": "rp-9",
                 "reason": "INVALID_PHONE_NUMBER", "expiresAt": "2026-11-02T12:20:00Z"}
                """);

        verify(procesar).telefonoRechazado(new EventoCatedra("ev-2", "AdditionalInformationRejected", "rp-9",
                Instant.parse("2026-11-02T12:20:00Z"), null, "INVALID_PHONE_NUMBER"));
    }

    @Test
    void losResultadosLleganAlCasoDeUsoConElIdDeReservaYElMotivo() {
        listener.recibir("""
                {"eventId": "ev-3", "eventType": "AppointmentConfirmed", "reservationProcessId": "rp-9",
                 "requestEventId": "ev-pedido", "reservationId": 500, "professionalId": 15, "date": "2026-11-03",
                 "startTime": "09:00", "status": "CONFIRMED"}
                """);
        listener.recibir("""
                {"eventId": "ev-4", "eventType": "AppointmentProcessExpired", "reservationProcessId": "rp-9",
                 "reason": "HOLD_EXPIRED"}
                """);
        listener.recibir("""
                {"eventId": "ev-5", "eventType": "AppointmentProcessInvalid", "reservationProcessId": "rp-9",
                 "reason": "SLOT_NOT_AVAILABLE"}
                """);
        listener.recibir("""
                {"eventId": "ev-6", "eventType": "AppointmentCancelled", "reservationProcessId": "rp-9",
                 "reservationId": 500, "reason": null}
                """);

        verify(procesar).confirmado(new EventoCatedra("ev-3", "AppointmentConfirmed", "rp-9", null, "500", null));
        verify(procesar).vencido(
                new EventoCatedra("ev-4", "AppointmentProcessExpired", "rp-9", null, null, "HOLD_EXPIRED"));
        verify(procesar).invalido(
                new EventoCatedra("ev-5", "AppointmentProcessInvalid", "rp-9", null, null, "SLOT_NOT_AVAILABLE"));
        verify(procesar).cancelado(new EventoCatedra("ev-6", "AppointmentCancelled", "rp-9", null, "500", null));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{esto no es json",
        "null",
        "{\"eventType\": \"AdditionalInformationRequested\", \"reservationProcessId\": \"rp-9\"}",
        "{\"eventId\": \"ev-1\", \"eventType\": \"AdditionalInformationRequested\"}",
        "{\"eventId\": \"ev-3\", \"eventType\": \"OtroEvento\", \"reservationProcessId\": \"rp-9\"}",
        "{\"eventId\": \"ev-4\", \"reservationProcessId\": \"rp-9\"}"
    })
    void loQueNoSePuedeProcesarSeDescartaSinFallar(String mensaje) {
        assertThatCode(() -> listener.recibir(mensaje)).doesNotThrowAnyException();

        verifyNoInteractions(procesar);
    }
}

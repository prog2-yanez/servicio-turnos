package prog2.servicioturnos.reservas.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import prog2.servicioturnos.reservas.domain.exception.EnvioNoConfirmadoException;
import prog2.servicioturnos.reservas.domain.model.Telefono;
import prog2.servicioturnos.reservas.domain.model.TelefonoInformado;
import tools.jackson.databind.json.JsonMapper;

class PublicadorTelefonoKafkaTests {

    private static final TelefonoInformado TELEFONO = new TelefonoInformado("ev-nuevo", "rp-9", "77", "ev-pedido",
            new Telefono("+542615551234"), Instant.parse("2026-11-02T12:01:00Z"));

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);

    @Test
    void publicaElEventoDelContratoConLaKeyDelProceso() throws Exception {
        when(kafka.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        publicador("telefono-prueba", "grupo-prueba").publicar(TELEFONO);

        ArgumentCaptor<String> mensaje = ArgumentCaptor.forClass(String.class);
        verify(kafka).send(eq("telefono-prueba"), eq("rp-9"), mensaje.capture());
        JSONAssert.assertEquals("""
                {"eventId": "ev-nuevo", "eventType": "AdditionalInformationSubmitted", "groupId": "grupo-prueba",
                 "reservationProcessId": "rp-9", "holdId": 77, "requestEventId": "ev-pedido",
                 "information": {"phoneNumber": "+542615551234"},
                 "occurredAt": "2026-11-02T12:01:00Z", "schemaVersion": 1}
                """, mensaje.getValue(), JSONCompareMode.STRICT);
    }

    @Test
    void unHoldIdQueNoEsNumeroSeMandaComoTexto() throws Exception {
        when(kafka.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));
        TelefonoInformado conTexto = new TelefonoInformado("ev-nuevo", "rp-9", "h-77", "ev-pedido",
                new Telefono("2615551234"), Instant.parse("2026-11-02T12:01:00Z"));

        publicador("telefono-prueba", "grupo-prueba").publicar(conTexto);

        ArgumentCaptor<String> mensaje = ArgumentCaptor.forClass(String.class);
        verify(kafka).send(anyString(), anyString(), mensaje.capture());
        JSONAssert.assertEquals("{\"holdId\": \"h-77\"}", mensaje.getValue(), JSONCompareMode.LENIENT);
    }

    @Test
    void siKafkaNoConfirmaEsEnvioNoConfirmado() {
        when(kafka.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker caído")));

        assertThatThrownBy(() -> publicador("telefono-prueba", "grupo-prueba").publicar(TELEFONO))
                .isInstanceOf(EnvioNoConfirmadoException.class);
    }

    @Test
    void siElEnvioFallaAlArmarseEsEnvioNoConfirmado() {
        when(kafka.send(anyString(), anyString(), anyString())).thenThrow(new IllegalStateException("sin broker"));

        assertThatThrownBy(() -> publicador("telefono-prueba", "grupo-prueba").publicar(TELEFONO))
                .isInstanceOf(EnvioNoConfirmadoException.class);
    }

    @Test
    void sinTopicOSinGrupoNoIntentaPublicar() {
        assertThatThrownBy(() -> publicador("", "grupo-prueba").publicar(TELEFONO))
                .isInstanceOf(EnvioNoConfirmadoException.class);
        assertThatThrownBy(() -> publicador("telefono-prueba", " ").publicar(TELEFONO))
                .isInstanceOf(EnvioNoConfirmadoException.class);
        verifyNoInteractions(kafka);
    }

    @Test
    void siSeInterrumpeLaEsperaEsEnvioNoConfirmado() {
        when(kafka.send(anyString(), anyString(), anyString())).thenReturn(new CompletableFuture<>());
        Thread.currentThread().interrupt();

        assertThatThrownBy(() -> publicador("telefono-prueba", "grupo-prueba").publicar(TELEFONO))
                .isInstanceOf(EnvioNoConfirmadoException.class);
        // Limpia la marca de interrupción para no afectar a las otras pruebas.
        Thread.interrupted();
    }

    private PublicadorTelefonoKafka publicador(String topic, String grupo) {
        return new PublicadorTelefonoKafka(kafka, JsonMapper.builder().build(), topic, grupo);
    }
}

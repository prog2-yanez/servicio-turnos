package prog2.servicioturnos.reservas.infrastructure.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import prog2.servicioturnos.reservas.domain.model.EstadoProceso;
import prog2.servicioturnos.reservas.domain.model.ProcesoReserva;
import prog2.servicioturnos.reservas.domain.ports.in.EnviarTelefonoUseCase;
import prog2.servicioturnos.reservas.domain.ports.in.IniciarReservaUseCase;
import prog2.servicioturnos.reservas.infrastructure.web.dto.EnviarTelefonoRequest;
import prog2.servicioturnos.reservas.infrastructure.web.dto.IniciarReservaRequest;
import prog2.servicioturnos.reservas.infrastructure.web.dto.ProcesoReservaResponse;

@RestController
@RequestMapping("/api/v1/reservas")
public class ReservaController {

    private final IniciarReservaUseCase iniciarReserva;
    private final EnviarTelefonoUseCase enviarTelefono;

    public ReservaController(IniciarReservaUseCase iniciarReserva, EnviarTelefonoUseCase enviarTelefono) {
        this.iniciarReserva = iniciarReserva;
        this.enviarTelefono = enviarTelefono;
    }

    // El usuario sale del token, nunca del cuerpo. 201 si quedó esperando el teléfono; 202 si la cátedra no
    // contestó y el proceso quedó INCIERTO (todavía no se sabe el resultado).
    @PostMapping
    public ResponseEntity<ProcesoReservaResponse> iniciar(
            @AuthenticationPrincipal Jwt token, @Valid @RequestBody IniciarReservaRequest pedido) {
        ProcesoReserva proceso = iniciarReserva.iniciar(token.getSubject(), pedido.aPedido());
        HttpStatus estado = proceso.estado() == EstadoProceso.INCIERTO ? HttpStatus.ACCEPTED : HttpStatus.CREATED;
        return ResponseEntity.status(estado).body(ProcesoReservaResponse.desde(proceso));
    }

    // 202: el teléfono salió hacia la cátedra, pero el resultado de la reserva llega después por Kafka.
    @PostMapping("/{id}/telefono")
    public ResponseEntity<ProcesoReservaResponse> enviarTelefono(@AuthenticationPrincipal Jwt token,
            @PathVariable long id, @Valid @RequestBody EnviarTelefonoRequest pedido) {
        ProcesoReserva proceso = enviarTelefono.enviar(token.getSubject(), id, pedido.telefono());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ProcesoReservaResponse.desde(proceso));
    }
}

package prog2.servicioturnos.reservas.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EnviarTelefonoRequest(
        @NotBlank(message = "es obligatorio") @Size(max = 30, message = "puede tener hasta 30 caracteres")
        String telefono) {
}

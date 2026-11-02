package prog2.servicioturnos.reservas.infrastructure.web.dto;

import jakarta.validation.constraints.Size;

// El motivo es opcional y se le pasa a la cátedra, que acepta hasta 500 caracteres.
public record CancelacionRequest(
        @Size(max = 500, message = "puede tener hasta 500 caracteres") String motivo) {

    public String motivoInformado() {
        return motivo == null || motivo.isBlank() ? null : motivo.strip();
    }
}

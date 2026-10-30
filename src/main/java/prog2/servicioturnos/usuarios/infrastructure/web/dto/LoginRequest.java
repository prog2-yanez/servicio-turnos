package prog2.servicioturnos.usuarios.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import prog2.servicioturnos.usuarios.domain.model.Credenciales;

// Mismos nombres que el login de JHipster y que /api/authenticate de la cátedra.
public record LoginRequest(
        @NotBlank(message = "es obligatorio") String username,
        @NotBlank(message = "es obligatoria") String password) {

    public Credenciales aCredenciales() {
        return new Credenciales(username, password);
    }

    @Override
    public String toString() {
        return "LoginRequest[username=" + username + ", password=****]";
    }
}

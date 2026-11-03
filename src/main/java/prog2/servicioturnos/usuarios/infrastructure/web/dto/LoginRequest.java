package prog2.servicioturnos.usuarios.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import prog2.servicioturnos.usuarios.domain.model.Credenciales;

// Mismos nombres que el login de JHipster y que /api/authenticate de la cátedra. Los largos máximos son los mismos
// que en el registro: un valor más largo no puede ser una cuenta válida.
public record LoginRequest(
        @NotBlank(message = "es obligatorio")
        @Size(max = 50, message = "puede tener hasta 50 caracteres")
        String username,

        @NotBlank(message = "es obligatoria")
        @Size(max = 100, message = "puede tener hasta 100 caracteres")
        String password) {

    public Credenciales aCredenciales() {
        return new Credenciales(username, password);
    }

    @Override
    public String toString() {
        return "LoginRequest[username=" + username + ", password=****]";
    }
}

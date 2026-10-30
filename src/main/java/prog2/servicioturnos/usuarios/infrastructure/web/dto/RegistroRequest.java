package prog2.servicioturnos.usuarios.infrastructure.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import prog2.servicioturnos.usuarios.domain.model.DatosRegistro;

// Mismos campos y límites que el registro de JHipster. Id, estado, roles y auditoría no se aceptan.
public record RegistroRequest(
        @NotBlank(message = "es obligatorio")
        @Size(max = 50, message = "puede tener hasta 50 caracteres")
        @Pattern(regexp = "^[_.@A-Za-z0-9-]+$", message = "solo puede tener letras, números y _.@-")
        String login,

        @NotNull(message = "es obligatoria")
        @Size(min = 4, max = 100, message = "tiene que tener entre 4 y 100 caracteres")
        String password,

        @Size(max = 50, message = "puede tener hasta 50 caracteres")
        String firstName,

        @Size(max = 50, message = "puede tener hasta 50 caracteres")
        String lastName,

        @NotBlank(message = "es obligatorio")
        @Email(message = "tiene que ser un email válido")
        @Size(min = 5, max = 254, message = "tiene que tener entre 5 y 254 caracteres")
        String email,

        @Size(max = 256, message = "puede tener hasta 256 caracteres")
        String imageUrl,

        @Size(min = 2, max = 10, message = "tiene que tener entre 2 y 10 caracteres")
        String langKey) {

    static final String IDIOMA_POR_DEFECTO = "es";

    public DatosRegistro aDatos() {
        String idioma = langKey == null ? IDIOMA_POR_DEFECTO : langKey;
        return new DatosRegistro(login, password, firstName, lastName, email, imageUrl, idioma);
    }

    @Override
    public String toString() {
        return "RegistroRequest[login=" + login + ", email=" + email + ", password=****]";
    }
}

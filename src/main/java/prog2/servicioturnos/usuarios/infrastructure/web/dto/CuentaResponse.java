package prog2.servicioturnos.usuarios.infrastructure.web.dto;

import java.util.Set;
import java.util.stream.Collectors;
import prog2.servicioturnos.usuarios.domain.model.Rol;
import prog2.servicioturnos.usuarios.domain.model.Usuario;

// Mismos nombres que la cuenta de JHipster. Nunca incluye el hash de la contraseña.
public record CuentaResponse(
        Long id, String login, String firstName, String lastName, String email,
        String imageUrl, String langKey, boolean activated, Set<String> authorities) {

    public static CuentaResponse desde(Usuario usuario) {
        return new CuentaResponse(usuario.id(), usuario.login(), usuario.nombre(), usuario.apellido(),
                usuario.email(), usuario.imagenUrl(), usuario.idioma(), usuario.activado(),
                usuario.roles().stream().map(Rol::nombre).collect(Collectors.toSet()));
    }
}

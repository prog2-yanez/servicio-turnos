package prog2.servicioturnos.usuarios.domain.model;

import java.time.Instant;
import java.util.Set;

// Cuenta de usuario. Id, estado, roles y auditoría los decide el backend, nunca quien se registra.
public record Usuario(
        Long id, String login, String contraseniaHash, String nombre, String apellido, String email,
        String imagenUrl, boolean activado, String idioma, Set<Rol> roles,
        String creadoPor, Instant creadoEn, String modificadoPor, Instant modificadoEn) {

    public Usuario {
        roles = Set.copyOf(roles);
    }

    // La cuenta nace activa y con el rol de usuario: no hay verificación por correo (CU-01).
    public static Usuario registrar(DatosRegistro datos, String contraseniaHash, Instant ahora) {
        return new Usuario(null, datos.login(), contraseniaHash, datos.nombre(), datos.apellido(), datos.email(),
                datos.imagenUrl(), true, datos.idioma(), Set.of(Rol.USUARIO),
                datos.login(), ahora, datos.login(), ahora);
    }

    public boolean tieneRol(Rol rol) {
        return roles.contains(rol);
    }
}

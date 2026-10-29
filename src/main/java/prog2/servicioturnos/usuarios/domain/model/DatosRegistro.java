package prog2.servicioturnos.usuarios.domain.model;

import java.util.Locale;

// Lo que manda la persona al registrarse. login y email se normalizan para que la unicidad no dependa de mayúsculas.
public record DatosRegistro(
        String login, String contrasenia, String nombre, String apellido,
        String email, String imagenUrl, String idioma) {

    public DatosRegistro {
        login = enMinusculas(login);
        email = enMinusculas(email);
    }

    private static String enMinusculas(String texto) {
        return texto == null ? null : texto.strip().toLowerCase(Locale.ROOT);
    }
}

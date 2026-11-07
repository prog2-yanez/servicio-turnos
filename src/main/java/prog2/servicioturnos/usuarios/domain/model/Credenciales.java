package prog2.servicioturnos.usuarios.domain.model;

import java.util.Locale;

// Login y contraseña con los que alguien quiere iniciar sesión. El login se compara en minúsculas, como se guardó.
public record Credenciales(String login, String contrasenia) {

    public Credenciales {
        login = login == null ? null : login.strip().toLowerCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return "Credenciales[login=" + login + ", contrasenia=****]";
    }
}

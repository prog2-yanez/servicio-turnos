package prog2.servicioturnos.usuarios.domain.model;

// Roles con el mismo nombre que en JHipster, que es como viajan dentro del JWT.
public enum Rol {

    USUARIO("ROLE_USER"),
    ADMINISTRADOR("ROLE_ADMIN");

    private final String nombre;

    Rol(String nombre) {
        this.nombre = nombre;
    }

    public String nombre() {
        return nombre;
    }
}

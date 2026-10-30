package prog2.servicioturnos.usuarios.domain.ports.out;

public interface CifradorContrasenias {

    String cifrar(String contrasenia);

    boolean coincide(String contrasenia, String hash);
}

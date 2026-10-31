package prog2.servicioturnos.usuarios.infrastructure.seguridad;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Component;
import prog2.servicioturnos.usuarios.domain.ports.out.CifradorContrasenias;

// Argon2id con los parámetros recomendados por Spring Security: 16 MB de memoria, 2 iteraciones y sal aleatoria.
@Component
public class CifradorArgon2id implements CifradorContrasenias {

    private final Argon2PasswordEncoder codificador = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();

    @Override
    public String cifrar(String contrasenia) {
        return codificador.encode(contrasenia);
    }

    @Override
    public boolean coincide(String contrasenia, String hash) {
        return codificador.matches(contrasenia, hash);
    }
}

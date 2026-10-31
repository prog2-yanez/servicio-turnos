package prog2.servicioturnos.usuarios.domain.ports.out;

import java.util.Optional;
import prog2.servicioturnos.usuarios.domain.model.Usuario;

public interface UsuarioRepository {

    boolean existeLogin(String login);

    boolean existeEmail(String email);

    Usuario guardar(Usuario usuario);

    Optional<Usuario> buscarPorLogin(String login);
}

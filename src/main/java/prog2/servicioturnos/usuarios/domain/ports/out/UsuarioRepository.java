package prog2.servicioturnos.usuarios.domain.ports.out;

import prog2.servicioturnos.usuarios.domain.model.Usuario;

public interface UsuarioRepository {

    boolean existeLogin(String login);

    boolean existeEmail(String email);

    Usuario guardar(Usuario usuario);
}

package prog2.servicioturnos.usuarios.domain.ports.in;

import prog2.servicioturnos.usuarios.domain.model.Usuario;

public interface ConsultarCuentaUseCase {

    Usuario consultar(String login);
}

package prog2.servicioturnos.usuarios.domain.ports.out;

import prog2.servicioturnos.usuarios.domain.model.TokenAcceso;
import prog2.servicioturnos.usuarios.domain.model.Usuario;

public interface EmisorTokens {

    TokenAcceso emitir(Usuario usuario);
}

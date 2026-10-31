package prog2.servicioturnos.usuarios.domain.ports.in;

import prog2.servicioturnos.usuarios.domain.model.DatosRegistro;
import prog2.servicioturnos.usuarios.domain.model.TokenAcceso;

public interface RegistrarUsuarioUseCase {

    TokenAcceso registrar(DatosRegistro datos);
}

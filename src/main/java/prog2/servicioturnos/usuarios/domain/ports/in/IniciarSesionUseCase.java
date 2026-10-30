package prog2.servicioturnos.usuarios.domain.ports.in;

import prog2.servicioturnos.usuarios.domain.model.Credenciales;
import prog2.servicioturnos.usuarios.domain.model.TokenAcceso;

public interface IniciarSesionUseCase {

    TokenAcceso iniciar(Credenciales credenciales);
}

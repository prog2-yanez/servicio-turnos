package prog2.servicioturnos.usuarios.application.usecases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import prog2.servicioturnos.usuarios.domain.exception.CuentaNoEncontradaException;
import prog2.servicioturnos.usuarios.domain.model.Usuario;
import prog2.servicioturnos.usuarios.domain.ports.in.ConsultarCuentaUseCase;
import prog2.servicioturnos.usuarios.domain.ports.out.UsuarioRepository;

@Service
public class ConsultarCuentaService implements ConsultarCuentaUseCase {

    private final UsuarioRepository usuarios;

    public ConsultarCuentaService(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    @Transactional(readOnly = true)
    public Usuario consultar(String login) {
        return usuarios.buscarPorLogin(login).orElseThrow(() -> new CuentaNoEncontradaException(login));
    }
}

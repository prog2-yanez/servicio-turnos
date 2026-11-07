package prog2.servicioturnos.usuarios.application.usecases;

import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import prog2.servicioturnos.usuarios.domain.exception.EmailRepetidoException;
import prog2.servicioturnos.usuarios.domain.exception.LoginRepetidoException;
import prog2.servicioturnos.usuarios.domain.model.DatosRegistro;
import prog2.servicioturnos.usuarios.domain.model.TokenAcceso;
import prog2.servicioturnos.usuarios.domain.model.Usuario;
import prog2.servicioturnos.usuarios.domain.ports.in.RegistrarUsuarioUseCase;
import prog2.servicioturnos.usuarios.domain.ports.out.CifradorContrasenias;
import prog2.servicioturnos.usuarios.domain.ports.out.EmisorTokens;
import prog2.servicioturnos.usuarios.domain.ports.out.UsuarioRepository;

// CU-01: crea la cuenta activa y devuelve el JWT, así la app queda con la sesión iniciada.
@Service
public class RegistrarUsuarioService implements RegistrarUsuarioUseCase {

    private final UsuarioRepository usuarios;
    private final CifradorContrasenias cifrador;
    private final EmisorTokens emisor;
    private final Clock reloj;

    public RegistrarUsuarioService(
            UsuarioRepository usuarios, CifradorContrasenias cifrador, EmisorTokens emisor, Clock reloj) {
        this.usuarios = usuarios;
        this.cifrador = cifrador;
        this.emisor = emisor;
        this.reloj = reloj;
    }

    @Override
    @Transactional
    public TokenAcceso registrar(DatosRegistro datos) {
        if (usuarios.existeLogin(datos.login())) {
            throw new LoginRepetidoException(datos.login());
        }
        if (usuarios.existeEmail(datos.email())) {
            throw new EmailRepetidoException();
        }
        String hash = cifrador.cifrar(datos.contrasenia());
        Usuario guardado = usuarios.guardar(Usuario.registrar(datos, hash, reloj.instant()));
        return emisor.emitir(guardado);
    }
}

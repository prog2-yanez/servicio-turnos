package prog2.servicioturnos.usuarios.application.usecases;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import prog2.servicioturnos.usuarios.domain.exception.CredencialesInvalidasException;
import prog2.servicioturnos.usuarios.domain.model.Credenciales;
import prog2.servicioturnos.usuarios.domain.model.TokenAcceso;
import prog2.servicioturnos.usuarios.domain.model.Usuario;
import prog2.servicioturnos.usuarios.domain.ports.in.IniciarSesionUseCase;
import prog2.servicioturnos.usuarios.domain.ports.out.CifradorContrasenias;
import prog2.servicioturnos.usuarios.domain.ports.out.EmisorTokens;
import prog2.servicioturnos.usuarios.domain.ports.out.UsuarioRepository;

// CU-02: valida login y contraseña y emite el JWT.
@Service
public class IniciarSesionService implements IniciarSesionUseCase {

    private final UsuarioRepository usuarios;
    private final CifradorContrasenias cifrador;
    private final EmisorTokens emisor;

    // Si el login no existe se compara contra este hash igual, para que la respuesta tarde lo mismo
    // y no se pueda adivinar qué cuentas existen midiendo el tiempo.
    private final String hashDeRelleno;

    public IniciarSesionService(UsuarioRepository usuarios, CifradorContrasenias cifrador, EmisorTokens emisor) {
        this.usuarios = usuarios;
        this.cifrador = cifrador;
        this.emisor = emisor;
        this.hashDeRelleno = cifrador.cifrar(UUID.randomUUID().toString());
    }

    @Override
    @Transactional(readOnly = true)
    public TokenAcceso iniciar(Credenciales credenciales) {
        Optional<Usuario> usuario = usuarios.buscarPorLogin(credenciales.login());
        String hash = usuario.map(Usuario::contraseniaHash).orElse(hashDeRelleno);
        boolean coincide = cifrador.coincide(credenciales.contrasenia(), hash);
        return usuario
                .filter(encontrado -> coincide && encontrado.activado())
                .map(emisor::emitir)
                .orElseThrow(CredencialesInvalidasException::new);
    }
}

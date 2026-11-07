package prog2.servicioturnos.usuarios.infrastructure.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import prog2.servicioturnos.usuarios.domain.ports.in.ConsultarCuentaUseCase;
import prog2.servicioturnos.usuarios.domain.ports.in.IniciarSesionUseCase;
import prog2.servicioturnos.usuarios.domain.ports.in.RegistrarUsuarioUseCase;
import prog2.servicioturnos.usuarios.infrastructure.web.dto.CuentaResponse;
import prog2.servicioturnos.usuarios.infrastructure.web.dto.LoginRequest;
import prog2.servicioturnos.usuarios.infrastructure.web.dto.RegistroRequest;
import prog2.servicioturnos.usuarios.infrastructure.web.dto.TokenResponse;

@RestController
@RequestMapping("/api/v1/cuenta")
public class CuentaController {

    private final RegistrarUsuarioUseCase registrarUsuario;
    private final IniciarSesionUseCase iniciarSesion;
    private final ConsultarCuentaUseCase consultarCuenta;

    public CuentaController(RegistrarUsuarioUseCase registrarUsuario, IniciarSesionUseCase iniciarSesion,
            ConsultarCuentaUseCase consultarCuenta) {
        this.registrarUsuario = registrarUsuario;
        this.iniciarSesion = iniciarSesion;
        this.consultarCuenta = consultarCuenta;
    }

    // El login sale del token ya validado, nunca de un dato que mande el cliente.
    @GetMapping
    public CuentaResponse consultar(@AuthenticationPrincipal Jwt token) {
        return CuentaResponse.desde(consultarCuenta.consultar(token.getSubject()));
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse registrar(@Valid @RequestBody RegistroRequest pedido) {
        return TokenResponse.desde(registrarUsuario.registrar(pedido.aDatos()));
    }

    @PostMapping("/login")
    public TokenResponse iniciarSesion(@Valid @RequestBody LoginRequest pedido) {
        return TokenResponse.desde(iniciarSesion.iniciar(pedido.aCredenciales()));
    }
}

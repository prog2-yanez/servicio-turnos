package prog2.servicioturnos.usuarios.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import prog2.servicioturnos.usuarios.domain.exception.CredencialesInvalidasException;
import prog2.servicioturnos.usuarios.domain.model.Credenciales;
import prog2.servicioturnos.usuarios.domain.model.Rol;
import prog2.servicioturnos.usuarios.domain.model.TokenAcceso;
import prog2.servicioturnos.usuarios.domain.model.Usuario;
import prog2.servicioturnos.usuarios.domain.ports.out.CifradorContrasenias;
import prog2.servicioturnos.usuarios.domain.ports.out.EmisorTokens;
import prog2.servicioturnos.usuarios.domain.ports.out.UsuarioRepository;

class IniciarSesionServiceTests {

    private static final Instant AHORA = Instant.parse("2026-10-30T12:00:00Z");

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final CifradorContrasenias cifrador = mock(CifradorContrasenias.class);
    private final EmisorTokens emisor = mock(EmisorTokens.class);
    private IniciarSesionService servicio;

    @BeforeEach
    void crearServicio() {
        when(cifrador.cifrar(any())).thenReturn("hash-de-relleno");
        servicio = new IniciarSesionService(usuarios, cifrador, emisor);
    }

    @Test
    void conCredencialesCorrectasDevuelveElToken() {
        Usuario ana = usuario(true);
        TokenAcceso token = new TokenAcceso("jwt", AHORA.plusSeconds(3600));
        when(usuarios.buscarPorLogin("ana")).thenReturn(Optional.of(ana));
        when(cifrador.coincide("secreta", "hash-de-ana")).thenReturn(true);
        when(emisor.emitir(ana)).thenReturn(token);

        assertThat(servicio.iniciar(new Credenciales("ANA", "secreta"))).isEqualTo(token);
    }

    @Test
    void conLaContraseniaErroneaNoEmiteToken() {
        when(usuarios.buscarPorLogin("ana")).thenReturn(Optional.of(usuario(true)));
        when(cifrador.coincide("otra", "hash-de-ana")).thenReturn(false);

        assertThatThrownBy(() -> servicio.iniciar(new Credenciales("ana", "otra")))
                .isInstanceOf(CredencialesInvalidasException.class);
        verify(emisor, never()).emitir(any());
    }

    @Test
    void unUsuarioInexistenteIgualCalculaElHashYRespondeLoMismo() {
        when(usuarios.buscarPorLogin("nadie")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.iniciar(new Credenciales("nadie", "secreta")))
                .isInstanceOf(CredencialesInvalidasException.class)
                .hasMessage("Usuario o contraseña incorrectos");
        verify(cifrador).coincide("secreta", "hash-de-relleno");
        verify(emisor, never()).emitir(any());
    }

    @Test
    void unaCuentaInactivaNoPuedeIniciarSesion() {
        when(usuarios.buscarPorLogin("ana")).thenReturn(Optional.of(usuario(false)));
        when(cifrador.coincide("secreta", "hash-de-ana")).thenReturn(true);

        assertThatThrownBy(() -> servicio.iniciar(new Credenciales("ana", "secreta")))
                .isInstanceOf(CredencialesInvalidasException.class);
        verify(emisor, never()).emitir(any());
    }

    private static Usuario usuario(boolean activado) {
        return new Usuario(1L, "ana", "hash-de-ana", "Ana", "Gomez", "ana@mail.com", null, activado, "es",
                Set.of(Rol.USUARIO), "ana", AHORA, "ana", AHORA);
    }
}

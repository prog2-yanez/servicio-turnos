package prog2.servicioturnos.usuarios.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import prog2.servicioturnos.usuarios.domain.exception.EmailRepetidoException;
import prog2.servicioturnos.usuarios.domain.exception.LoginRepetidoException;
import prog2.servicioturnos.usuarios.domain.model.DatosRegistro;
import prog2.servicioturnos.usuarios.domain.model.Rol;
import prog2.servicioturnos.usuarios.domain.model.TokenAcceso;
import prog2.servicioturnos.usuarios.domain.model.Usuario;
import prog2.servicioturnos.usuarios.domain.ports.out.CifradorContrasenias;
import prog2.servicioturnos.usuarios.domain.ports.out.EmisorTokens;
import prog2.servicioturnos.usuarios.domain.ports.out.UsuarioRepository;

class RegistrarUsuarioServiceTests {

    private static final Instant AHORA = Instant.parse("2026-10-30T12:00:00Z");
    private static final DatosRegistro DATOS =
            new DatosRegistro("ana", "secreta", "Ana", "Gomez", "ana@mail.com", null, "es");

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final CifradorContrasenias cifrador = mock(CifradorContrasenias.class);
    private final EmisorTokens emisor = mock(EmisorTokens.class);
    private final RegistrarUsuarioService servicio =
            new RegistrarUsuarioService(usuarios, cifrador, emisor, Clock.fixed(AHORA, ZoneOffset.UTC));

    @Test
    void guardaElUsuarioConLaContraseniaCifradaYDevuelveElToken() {
        TokenAcceso token = new TokenAcceso("jwt", AHORA.plusSeconds(3600));
        when(cifrador.cifrar("secreta")).thenReturn("hash-argon2");
        when(usuarios.guardar(any())).thenAnswer(pedido -> pedido.getArgument(0));
        when(emisor.emitir(any())).thenReturn(token);

        assertThat(servicio.registrar(DATOS)).isEqualTo(token);

        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).guardar(guardado.capture());
        assertThat(guardado.getValue().contraseniaHash()).isEqualTo("hash-argon2");
        assertThat(guardado.getValue().tieneRol(Rol.USUARIO)).isTrue();
        assertThat(guardado.getValue().creadoEn()).isEqualTo(AHORA);
    }

    @Test
    void unLoginRepetidoSeRechazaSinCifrarNiGuardar() {
        when(usuarios.existeLogin("ana")).thenReturn(true);

        assertThatThrownBy(() -> servicio.registrar(DATOS)).isInstanceOf(LoginRepetidoException.class);
        verify(cifrador, never()).cifrar(any());
        verify(usuarios, never()).guardar(any());
    }

    @Test
    void unEmailRepetidoSeRechaza() {
        when(usuarios.existeEmail("ana@mail.com")).thenReturn(true);

        assertThatThrownBy(() -> servicio.registrar(DATOS)).isInstanceOf(EmailRepetidoException.class);
        verify(usuarios, never()).guardar(any());
    }
}

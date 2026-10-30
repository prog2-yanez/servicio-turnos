package prog2.servicioturnos.usuarios.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import prog2.servicioturnos.usuarios.domain.exception.CuentaNoEncontradaException;
import prog2.servicioturnos.usuarios.domain.model.Rol;
import prog2.servicioturnos.usuarios.domain.model.Usuario;
import prog2.servicioturnos.usuarios.domain.ports.out.UsuarioRepository;

class ConsultarCuentaServiceTests {

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final ConsultarCuentaService servicio = new ConsultarCuentaService(usuarios);

    @Test
    void devuelveLaCuentaDelLogin() {
        Instant ahora = Instant.parse("2026-10-30T12:00:00Z");
        Usuario ana = new Usuario(1L, "ana", "hash", "Ana", "Gomez", "ana@mail.com", null, true, "es",
                Set.of(Rol.USUARIO), "ana", ahora, "ana", ahora);
        when(usuarios.buscarPorLogin("ana")).thenReturn(Optional.of(ana));

        assertThat(servicio.consultar("ana")).isEqualTo(ana);
    }

    @Test
    void unaCuentaQueYaNoExisteDaNoEncontrada() {
        when(usuarios.buscarPorLogin("borrada")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.consultar("borrada")).isInstanceOf(CuentaNoEncontradaException.class);
    }
}

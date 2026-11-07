package prog2.servicioturnos.reservas.infrastructure.usuarios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import prog2.servicioturnos.reservas.domain.model.Paciente;
import prog2.servicioturnos.usuarios.domain.model.DatosRegistro;
import prog2.servicioturnos.usuarios.domain.model.Usuario;
import prog2.servicioturnos.usuarios.domain.ports.in.ConsultarCuentaUseCase;

class CuentasPacientesAdapterTests {

    @Test
    void tomaElIdYElNombreDeLaCuenta() {
        ConsultarCuentaUseCase consultarCuenta = mock(ConsultarCuentaUseCase.class);
        Usuario usuario = Usuario.registrar(
                new DatosRegistro("ana", "secreta", "Ana", "Gomez", "ana@mail.com", null, "es"), "hash",
                Instant.parse("2026-11-02T12:00:00Z"));
        Usuario conId = new Usuario(4L, usuario.login(), usuario.contraseniaHash(), usuario.nombre(),
                usuario.apellido(), usuario.email(), usuario.imagenUrl(), usuario.activado(), usuario.idioma(),
                usuario.roles(), usuario.creadoPor(), usuario.creadoEn(), usuario.modificadoPor(),
                usuario.modificadoEn());
        when(consultarCuenta.consultar("ana")).thenReturn(conId);

        assertThat(new CuentasPacientesAdapter(consultarCuenta).obtener("ana"))
                .isEqualTo(new Paciente(4, "Ana", "Gomez"));
    }
}

package prog2.servicioturnos.usuarios.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class UsuarioTests {

    private static final Instant AHORA = Instant.parse("2026-10-30T12:00:00Z");

    @Test
    void elRegistroDejaLaCuentaActivaConElRolDeUsuarioYLaAuditoria() {
        DatosRegistro datos = new DatosRegistro("ana", "secreta", "Ana", "Gomez", "ana@mail.com", null, "es");

        Usuario usuario = Usuario.registrar(datos, "hash", AHORA);

        assertThat(usuario.id()).isNull();
        assertThat(usuario.activado()).isTrue();
        assertThat(usuario.contraseniaHash()).isEqualTo("hash");
        assertThat(usuario.tieneRol(Rol.USUARIO)).isTrue();
        assertThat(usuario.tieneRol(Rol.ADMINISTRADOR)).isFalse();
        assertThat(usuario.creadoPor()).isEqualTo("ana");
        assertThat(usuario.creadoEn()).isEqualTo(AHORA);
        assertThat(usuario.modificadoEn()).isEqualTo(AHORA);
    }

    @Test
    void loginYEmailSeGuardanEnMinusculasYSinEspacios() {
        DatosRegistro datos = new DatosRegistro(" Ana.G ", "secreta", null, null, " Ana@Mail.COM ", null, "es");

        assertThat(datos.login()).isEqualTo("ana.g");
        assertThat(datos.email()).isEqualTo("ana@mail.com");
    }

    @Test
    void laContraseniaNoApareceAlImprimirLosDatos() {
        DatosRegistro datos = new DatosRegistro("ana", "secreta", null, null, "ana@mail.com", null, "es");

        assertThat(datos.toString()).doesNotContain("secreta").contains("****");
    }

    @Test
    void reconoceLosRolesPorSuNombreDeJhipster() {
        assertThat(Rol.desdeNombre("ROLE_USER")).isEqualTo(Rol.USUARIO);
        assertThat(Rol.desdeNombre("ROLE_ADMIN")).isEqualTo(Rol.ADMINISTRADOR);
        assertThatThrownBy(() -> Rol.desdeNombre("ROLE_INVENTADO")).isInstanceOf(IllegalArgumentException.class);
    }
}

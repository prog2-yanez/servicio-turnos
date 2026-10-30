package prog2.servicioturnos.usuarios.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import prog2.servicioturnos.TestcontainersConfiguration;
import prog2.servicioturnos.usuarios.domain.model.DatosRegistro;
import prog2.servicioturnos.usuarios.domain.model.Rol;
import prog2.servicioturnos.usuarios.domain.model.Usuario;
import prog2.servicioturnos.usuarios.infrastructure.persistence.mapper.UsuarioMapper;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, UsuarioMapper.class, UsuarioPersistenceAdapter.class})
class UsuarioPersistenciaTests {

    @Autowired
    private UsuarioPersistenceAdapter adaptador;

    @Test
    void guardaElUsuarioConSusRolesYLoEncuentraPorLoginYEmail() {
        DatosRegistro datos = new DatosRegistro("ana", "secreta", "Ana", "Gomez", "ana@mail.com", null, "es");

        Usuario guardado = adaptador.guardar(Usuario.registrar(datos, "hash", Instant.parse("2026-10-30T12:00:00Z")));

        assertThat(guardado.id()).isNotNull();
        assertThat(guardado.roles()).containsExactly(Rol.USUARIO);
        assertThat(adaptador.existeLogin("ana")).isTrue();
        assertThat(adaptador.existeEmail("ana@mail.com")).isTrue();
        assertThat(adaptador.existeLogin("otra")).isFalse();
        assertThat(adaptador.existeEmail("otra@mail.com")).isFalse();
        assertThat(adaptador.buscarPorLogin("ana")).get().extracting(Usuario::email).isEqualTo("ana@mail.com");
        assertThat(adaptador.buscarPorLogin("otra")).isEmpty();
    }
}

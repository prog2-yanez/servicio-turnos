package prog2.servicioturnos.usuarios.infrastructure.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CifradorArgon2idTests {

    private final CifradorArgon2id cifrador = new CifradorArgon2id();

    @Test
    void elHashEsArgon2idYSoloCoincideConLaContraseniaOriginal() {
        String hash = cifrador.cifrar("secreta");

        assertThat(hash).startsWith("$argon2id$").doesNotContain("secreta");
        assertThat(cifrador.coincide("secreta", hash)).isTrue();
        assertThat(cifrador.coincide("otra", hash)).isFalse();
    }

    @Test
    void laMismaContraseniaDaHashesDistintosPorLaSal() {
        assertThat(cifrador.cifrar("secreta")).isNotEqualTo(cifrador.cifrar("secreta"));
    }
}

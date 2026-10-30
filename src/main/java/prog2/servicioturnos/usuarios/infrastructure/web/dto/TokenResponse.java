package prog2.servicioturnos.usuarios.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import prog2.servicioturnos.usuarios.domain.model.TokenAcceso;

// id_token como en JHipster y en la cátedra, para que la app lo lea igual en los dos casos.
public record TokenResponse(@JsonProperty("id_token") String idToken, Instant venceEn) {

    public static TokenResponse desde(TokenAcceso token) {
        return new TokenResponse(token.valor(), token.venceEn());
    }
}

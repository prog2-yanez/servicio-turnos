package prog2.servicioturnos.shared.catedra;

import java.io.IOException;
import java.util.Optional;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

// Lee el code de un error de la cátedra (contrato, sección 13). El mismo status puede significar cosas distintas:
// por ejemplo, un 404 puede ser un profesional inexistente o la cuenta técnica mal configurada.
@Component
public class LectorErrorCatedra {

    private final JsonMapper json;

    public LectorErrorCatedra(JsonMapper json) {
        this.json = json;
    }

    public Optional<String> codigo(ClientHttpResponse respuesta) {
        try {
            ErrorCatedraResponse error = json.readValue(respuesta.getBody(), ErrorCatedraResponse.class);
            return Optional.ofNullable(error).map(ErrorCatedraResponse::code);
        } catch (IOException | JacksonException e) {
            // Los 401 y 403 de Spring Security pueden venir sin cuerpo ni code.
            return Optional.empty();
        }
    }

    // La cuenta técnica venció o no corresponde: es un problema de configuración, no del usuario.
    public static boolean esProblemaDeCuentaTecnica(int estado, String codigo) {
        return estado == 401 || estado == 403 || "STUDENT_INTEGRATION_NOT_FOUND".equals(codigo);
    }
}

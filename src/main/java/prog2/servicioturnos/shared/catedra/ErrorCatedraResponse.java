package prog2.servicioturnos.shared.catedra;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// De los errores de la cátedra solo se lee el code; el resto puede traer datos internos y no se usa.
@JsonIgnoreProperties(ignoreUnknown = true)
record ErrorCatedraResponse(String code) {
}

package prog2.servicioturnos.disponibilidad.infrastructure.catedra.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// De los errores de la cátedra solo se lee el code; el resto puede traer datos internos y no se usa.
@JsonIgnoreProperties(ignoreUnknown = true)
public record ErrorCatedraResponse(String code) {
}

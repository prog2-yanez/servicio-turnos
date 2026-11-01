package prog2.servicioturnos.disponibilidad.infrastructure.catalogo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import prog2.servicioturnos.disponibilidad.domain.model.ProfesionalResumen;

// Respuesta paginada de GET /api/v1/profesionales del catálogo (ADR 0002).
@JsonIgnoreProperties(ignoreUnknown = true)
public record PaginaProfesionalesResponse(List<Profesional> content, Pagina page) {

    public boolean esUltima() {
        return page == null || page.number() + 1 >= page.totalPages();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Profesional(long id, long categoriaId, String nombre, String apellido) {

        public ProfesionalResumen aDominio() {
            return new ProfesionalResumen(id, categoriaId, nombre, apellido);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Pagina(int number, int totalPages) {
    }
}

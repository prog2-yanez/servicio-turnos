package prog2.servicioturnos.disponibilidad.domain.model;

import java.util.List;

// Datos vigentes de un profesional habilitado, tal como los informa el catálogo en este momento.
// versionCatalogo dice con qué versión del catálogo se validó un turno.
public record ProfesionalConAgenda(
        long id,
        long categoriaId,
        String nombre,
        String apellido,
        List<HorarioSemanal> horarios,
        long versionCatalogo) {

    public ProfesionalConAgenda {
        horarios = List.copyOf(horarios);
    }
}

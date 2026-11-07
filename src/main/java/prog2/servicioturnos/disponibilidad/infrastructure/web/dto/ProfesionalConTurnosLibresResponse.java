package prog2.servicioturnos.disponibilidad.infrastructure.web.dto;

import prog2.servicioturnos.disponibilidad.domain.model.ProfesionalConTurnosLibres;

public record ProfesionalConTurnosLibresResponse(
        long id, long categoriaId, String nombre, String apellido, int turnosLibres) {

    public static ProfesionalConTurnosLibresResponse desde(ProfesionalConTurnosLibres profesional) {
        return new ProfesionalConTurnosLibresResponse(profesional.id(), profesional.categoriaId(),
                profesional.nombre(), profesional.apellido(), profesional.turnosLibres());
    }
}

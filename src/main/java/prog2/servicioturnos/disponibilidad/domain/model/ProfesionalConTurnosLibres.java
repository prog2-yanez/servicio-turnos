package prog2.servicioturnos.disponibilidad.domain.model;

// Un profesional que tiene al menos un turno libre en la fecha pedida, y cuántos.
public record ProfesionalConTurnosLibres(long id, long categoriaId, String nombre, String apellido, int turnosLibres) {
}

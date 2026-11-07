package prog2.servicioturnos.disponibilidad.domain.model;

// Datos básicos de un profesional, como vienen en el listado del catálogo (sin agenda).
public record ProfesionalResumen(long id, long categoriaId, String nombre, String apellido) {
}

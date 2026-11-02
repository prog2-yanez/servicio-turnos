package prog2.servicioturnos.reservas.domain.model;

// Resumen de una ronda de recuperación: cuántos procesos abiertos se revisaron, cuántos quedaron resueltos (con
// estado final) y cuántos siguen esperando su resultado.
public record ResultadoRecuperacion(int revisados, int resueltos, int pendientes) {
}

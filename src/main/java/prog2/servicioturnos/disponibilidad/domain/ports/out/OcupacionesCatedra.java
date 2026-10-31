package prog2.servicioturnos.disponibilidad.domain.ports.out;

import java.time.LocalDate;
import java.util.List;
import prog2.servicioturnos.disponibilidad.domain.model.Ocupacion;

// Turnos ya tomados o retenidos en la cátedra. Una lista vacía es válida: el profesional tiene todo libre.
public interface OcupacionesCatedra {

    List<Ocupacion> obtener(long profesionalId, LocalDate fecha);
}

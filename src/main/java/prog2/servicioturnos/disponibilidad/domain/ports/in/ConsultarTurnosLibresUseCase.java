package prog2.servicioturnos.disponibilidad.domain.ports.in;

import java.time.LocalDate;
import prog2.servicioturnos.disponibilidad.domain.model.TurnosLibresDelProfesional;

public interface ConsultarTurnosLibresUseCase {

    TurnosLibresDelProfesional consultar(long profesionalId, LocalDate fecha);
}

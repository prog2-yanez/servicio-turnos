package prog2.servicioturnos.disponibilidad.domain.ports.in;

import java.time.LocalDate;
import java.util.List;
import prog2.servicioturnos.disponibilidad.domain.model.ProfesionalConTurnosLibres;

public interface BuscarProfesionalesConTurnosLibresUseCase {

    List<ProfesionalConTurnosLibres> buscar(long categoriaId, LocalDate fecha);
}

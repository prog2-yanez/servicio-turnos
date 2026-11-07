package prog2.servicioturnos.disponibilidad.domain.ports.out;

import java.util.List;
import prog2.servicioturnos.disponibilidad.domain.model.ProfesionalConAgenda;
import prog2.servicioturnos.disponibilidad.domain.model.ProfesionalResumen;

// Turnos no guarda copia del catálogo: cada vez que necesita un profesional se lo pide.
public interface CatalogoClient {

    ProfesionalConAgenda obtenerProfesional(long id);

    // Todos los profesionales habilitados de la categoría, recorriendo todas las páginas del catálogo.
    List<ProfesionalResumen> listarHabilitados(long categoriaId);
}

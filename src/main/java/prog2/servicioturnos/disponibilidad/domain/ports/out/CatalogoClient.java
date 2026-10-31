package prog2.servicioturnos.disponibilidad.domain.ports.out;

import prog2.servicioturnos.disponibilidad.domain.model.ProfesionalConAgenda;

// Turnos no guarda copia del catálogo: cada vez que necesita un profesional se lo pide.
public interface CatalogoClient {

    ProfesionalConAgenda obtenerProfesional(long id);
}

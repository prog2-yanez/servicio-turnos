package prog2.servicioturnos.disponibilidad.application.usecases;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import prog2.servicioturnos.config.TurnosProperties;
import prog2.servicioturnos.disponibilidad.domain.exception.FechaPasadaException;
import prog2.servicioturnos.disponibilidad.domain.model.Ocupacion;
import prog2.servicioturnos.disponibilidad.domain.model.ProfesionalConAgenda;
import prog2.servicioturnos.disponibilidad.domain.model.TurnoLibre;
import prog2.servicioturnos.disponibilidad.domain.model.TurnosLibresDelProfesional;
import prog2.servicioturnos.disponibilidad.domain.ports.in.ConsultarTurnosLibresUseCase;
import prog2.servicioturnos.disponibilidad.domain.ports.out.CatalogoClient;
import prog2.servicioturnos.disponibilidad.domain.ports.out.OcupacionesCatedra;
import prog2.servicioturnos.disponibilidad.domain.service.CalculadoraTurnosLibres;

// CU-03: combina la agenda vigente del catálogo con las ocupaciones de la cátedra.
@Service
public class ConsultarTurnosLibresService implements ConsultarTurnosLibresUseCase {

    private final CatalogoClient catalogo;
    private final OcupacionesCatedra ocupacionesCatedra;
    private final Clock reloj;
    private final TurnosProperties propiedades;

    public ConsultarTurnosLibresService(CatalogoClient catalogo, OcupacionesCatedra ocupacionesCatedra, Clock reloj,
            TurnosProperties propiedades) {
        this.catalogo = catalogo;
        this.ocupacionesCatedra = ocupacionesCatedra;
        this.reloj = reloj;
        this.propiedades = propiedades;
    }

    @Override
    public TurnosLibresDelProfesional consultar(long profesionalId, LocalDate fecha) {
        Instant ahora = reloj.instant();
        if (fecha.isBefore(LocalDate.ofInstant(ahora, propiedades.zonaHoraria()))) {
            throw new FechaPasadaException(fecha);
        }
        ProfesionalConAgenda profesional = catalogo.obtenerProfesional(profesionalId);
        List<TurnoLibre> libres = List.of();
        // Si no atiende ese día no hay nada que restar: no se le pregunta a la cátedra.
        if (CalculadoraTurnosLibres.atiendeEseDia(fecha, profesional.horarios())) {
            List<Ocupacion> ocupaciones = ocupacionesCatedra.obtener(profesionalId, fecha);
            libres = CalculadoraTurnosLibres.calcular(fecha, profesional.horarios(), ocupaciones, ahora,
                    propiedades.zonaHoraria());
        }
        return new TurnosLibresDelProfesional(profesionalId, fecha, profesional.versionCatalogo(), libres);
    }
}

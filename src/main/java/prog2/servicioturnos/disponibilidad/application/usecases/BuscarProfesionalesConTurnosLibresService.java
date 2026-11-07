package prog2.servicioturnos.disponibilidad.application.usecases;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import prog2.servicioturnos.config.TurnosProperties;
import prog2.servicioturnos.disponibilidad.domain.exception.FechaPasadaException;
import prog2.servicioturnos.disponibilidad.domain.exception.ProfesionalDeshabilitadoException;
import prog2.servicioturnos.disponibilidad.domain.exception.ProfesionalNoEncontradoException;
import prog2.servicioturnos.disponibilidad.domain.model.ProfesionalConTurnosLibres;
import prog2.servicioturnos.disponibilidad.domain.model.ProfesionalResumen;
import prog2.servicioturnos.disponibilidad.domain.model.TurnosLibresDelProfesional;
import prog2.servicioturnos.disponibilidad.domain.ports.in.BuscarProfesionalesConTurnosLibresUseCase;
import prog2.servicioturnos.disponibilidad.domain.ports.in.ConsultarTurnosLibresUseCase;
import prog2.servicioturnos.disponibilidad.domain.ports.out.CatalogoClient;
import prog2.servicioturnos.disponibilidad.domain.service.CalculadoraTurnosLibres;

// Disponible significa tener al menos un turno libre (aclaración del profesor): por eso el filtro vive en turnos.
@Service
public class BuscarProfesionalesConTurnosLibresService implements BuscarProfesionalesConTurnosLibresUseCase {

    private static final Logger log = LoggerFactory.getLogger(BuscarProfesionalesConTurnosLibresService.class);

    private final CatalogoClient catalogo;
    private final ConsultarTurnosLibresUseCase consultarTurnosLibres;
    private final Clock reloj;
    private final TurnosProperties propiedades;

    public BuscarProfesionalesConTurnosLibresService(CatalogoClient catalogo,
            ConsultarTurnosLibresUseCase consultarTurnosLibres, Clock reloj, TurnosProperties propiedades) {
        this.catalogo = catalogo;
        this.consultarTurnosLibres = consultarTurnosLibres;
        this.reloj = reloj;
        this.propiedades = propiedades;
    }

    // Si el catálogo o la cátedra no responden, falla toda la búsqueda: una lista parcial haría creer que los
    // que faltan no tienen turnos.
    @Override
    public List<ProfesionalConTurnosLibres> buscar(long categoriaId, LocalDate fecha) {
        if (CalculadoraTurnosLibres.esFechaPasada(fecha, reloj.instant(), propiedades.zonaHoraria())) {
            throw new FechaPasadaException(fecha);
        }
        List<ProfesionalConTurnosLibres> conTurnos = new ArrayList<>();
        for (ProfesionalResumen profesional : catalogo.listarHabilitados(categoriaId)) {
            try {
                TurnosLibresDelProfesional libres = consultarTurnosLibres.consultar(profesional.id(), fecha);
                if (!libres.turnos().isEmpty()) {
                    conTurnos.add(new ProfesionalConTurnosLibres(profesional.id(), profesional.categoriaId(),
                            profesional.nombre(), profesional.apellido(), libres.turnos().size()));
                }
            } catch (ProfesionalNoEncontradoException | ProfesionalDeshabilitadoException e) {
                // Cambió entre el listado y la consulta (por ejemplo, se deshabilitó): simplemente no se ofrece.
                log.info("Se saltea el profesional {}: {}", profesional.id(), e.codigo());
            }
        }
        return conTurnos;
    }
}

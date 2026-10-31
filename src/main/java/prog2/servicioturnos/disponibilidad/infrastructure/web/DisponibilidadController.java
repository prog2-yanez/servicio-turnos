package prog2.servicioturnos.disponibilidad.infrastructure.web;

import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import prog2.servicioturnos.disponibilidad.domain.ports.in.BuscarProfesionalesConTurnosLibresUseCase;
import prog2.servicioturnos.disponibilidad.domain.ports.in.ConsultarTurnosLibresUseCase;
import prog2.servicioturnos.disponibilidad.infrastructure.web.dto.ProfesionalConTurnosLibresResponse;
import prog2.servicioturnos.disponibilidad.infrastructure.web.dto.TurnosLibresResponse;

@RestController
@RequestMapping("/api/v1/disponibilidad/profesionales")
public class DisponibilidadController {

    private final ConsultarTurnosLibresUseCase consultarTurnosLibres;
    private final BuscarProfesionalesConTurnosLibresUseCase buscarConTurnosLibres;

    public DisponibilidadController(ConsultarTurnosLibresUseCase consultarTurnosLibres,
            BuscarProfesionalesConTurnosLibresUseCase buscarConTurnosLibres) {
        this.consultarTurnosLibres = consultarTurnosLibres;
        this.buscarConTurnosLibres = buscarConTurnosLibres;
    }

    // Solo los profesionales de la categoría que tienen al menos un turno libre en la fecha.
    @GetMapping
    public List<ProfesionalConTurnosLibresResponse> conTurnosLibres(
            @RequestParam long categoriaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return buscarConTurnosLibres.buscar(categoriaId, fecha).stream()
                .map(ProfesionalConTurnosLibresResponse::desde)
                .toList();
    }

    @GetMapping("/{id}")
    public TurnosLibresResponse turnosLibres(
            @PathVariable long id, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return TurnosLibresResponse.desde(consultarTurnosLibres.consultar(id, fecha));
    }
}

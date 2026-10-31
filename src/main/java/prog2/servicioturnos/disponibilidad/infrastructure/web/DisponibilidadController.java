package prog2.servicioturnos.disponibilidad.infrastructure.web;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import prog2.servicioturnos.disponibilidad.domain.ports.in.ConsultarTurnosLibresUseCase;
import prog2.servicioturnos.disponibilidad.infrastructure.web.dto.TurnosLibresResponse;

@RestController
@RequestMapping("/api/v1/disponibilidad/profesionales")
public class DisponibilidadController {

    private final ConsultarTurnosLibresUseCase consultarTurnosLibres;

    public DisponibilidadController(ConsultarTurnosLibresUseCase consultarTurnosLibres) {
        this.consultarTurnosLibres = consultarTurnosLibres;
    }

    @GetMapping("/{id}")
    public TurnosLibresResponse turnosLibres(
            @PathVariable long id, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return TurnosLibresResponse.desde(consultarTurnosLibres.consultar(id, fecha));
    }
}

package prog2.servicioturnos.disponibilidad.infrastructure.catalogo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import prog2.servicioturnos.disponibilidad.domain.model.HorarioSemanal;
import prog2.servicioturnos.disponibilidad.domain.model.ProfesionalConAgenda;

// Respuesta de GET /api/v1/profesionales/{id} del catálogo (ADR 0002). Los campos que no se usan se ignoran.
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProfesionalConAgendaResponse(
        long id, long categoriaId, String nombre, String apellido, List<Horario> horarios, long versionCatalogo) {

    public ProfesionalConAgenda aDominio() {
        List<HorarioSemanal> agenda = horarios == null ? List.of() : horarios.stream().map(Horario::aDominio).toList();
        return new ProfesionalConAgenda(id, categoriaId, nombre, apellido, agenda, versionCatalogo);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Horario(DayOfWeek diaSemana, LocalTime horaInicio, LocalTime horaFin, int duracionTurnoMinutos) {

        HorarioSemanal aDominio() {
            return new HorarioSemanal(diaSemana, horaInicio, horaFin, duracionTurnoMinutos);
        }
    }
}

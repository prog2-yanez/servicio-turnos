package prog2.servicioturnos.disponibilidad.domain.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import prog2.servicioturnos.disponibilidad.domain.model.EstadoOcupacion;
import prog2.servicioturnos.disponibilidad.domain.model.HorarioSemanal;
import prog2.servicioturnos.disponibilidad.domain.model.Ocupacion;
import prog2.servicioturnos.disponibilidad.domain.model.TurnoLibre;

// Reglas de la disponibilidad, sin Spring ni servicios externos: recibe todo lo que necesita y devuelve los libres.
public final class CalculadoraTurnosLibres {

    private CalculadoraTurnosLibres() {
    }

    // Los horarios del día de la semana de la fecha se parten en turnos de su duración; un sobrante que no llega a
    // un turno entero no se ofrece. Después se quitan los ocupados, los retenidos vigentes y los que ya empezaron.
    public static List<TurnoLibre> calcular(LocalDate fecha, List<HorarioSemanal> agenda, List<Ocupacion> ocupaciones,
            Instant ahora, ZoneId zona) {
        Set<LocalTime> ocupados = horasOcupadas(fecha, ocupaciones, ahora);
        LocalDateTime ahoraLocal = LocalDateTime.ofInstant(ahora, zona);
        TreeMap<LocalTime, TurnoLibre> libres = new TreeMap<>();
        for (TurnoLibre turno : turnosDeLaAgenda(fecha, agenda)) {
            boolean yaEmpezo = !fecha.atTime(turno.horaInicio()).isAfter(ahoraLocal);
            if (!ocupados.contains(turno.horaInicio()) && !yaEmpezo) {
                libres.putIfAbsent(turno.horaInicio(), turno);
            }
        }
        return List.copyOf(libres.values());
    }

    // Hoy se puede consultar (se ofrecen los turnos que todavía no empezaron); un día anterior, no.
    public static boolean esFechaPasada(LocalDate fecha, Instant ahora, ZoneId zona) {
        return fecha.isBefore(LocalDate.ofInstant(ahora, zona));
    }

    // Solo hace falta preguntar por las ocupaciones si la agenda tiene algún turno ese día.
    public static boolean atiendeEseDia(LocalDate fecha, List<HorarioSemanal> agenda) {
        return !turnosDeLaAgenda(fecha, agenda).isEmpty();
    }

    private static List<TurnoLibre> turnosDeLaAgenda(LocalDate fecha, List<HorarioSemanal> agenda) {
        return agenda.stream()
                .filter(horario -> horario.diaSemana() == fecha.getDayOfWeek())
                .filter(horario -> horario.duracionTurnoMinutos() > 0)
                .flatMap(horario -> partir(horario).stream())
                .toList();
    }

    private static List<TurnoLibre> partir(HorarioSemanal horario) {
        List<TurnoLibre> turnos = new ArrayList<>();
        LocalTime inicio = horario.horaInicio();
        while (true) {
            LocalTime fin = inicio.plusMinutes(horario.duracionTurnoMinutos());
            // fin.isBefore(inicio) detecta que el turno pasaría de medianoche.
            if (fin.isAfter(horario.horaFin()) || fin.isBefore(inicio)) {
                return turnos;
            }
            turnos.add(new TurnoLibre(inicio, fin));
            if (fin.equals(horario.horaFin())) {
                return turnos;
            }
            inicio = fin;
        }
    }

    // Un retenido cuyo vencimiento ya pasó se toma como libre: el hold ya no vale.
    private static Set<LocalTime> horasOcupadas(LocalDate fecha, List<Ocupacion> ocupaciones, Instant ahora) {
        return ocupaciones.stream()
                .filter(ocupacion -> ocupacion.fecha().equals(fecha))
                .filter(ocupacion -> ocupacion.estado() == EstadoOcupacion.CONFIRMADA
                        || ocupacion.venceEn() == null
                        || ocupacion.venceEn().isAfter(ahora))
                .map(Ocupacion::horaInicio)
                .collect(Collectors.toSet());
    }
}

package prog2.servicioturnos.disponibilidad.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import prog2.servicioturnos.disponibilidad.domain.model.EstadoOcupacion;
import prog2.servicioturnos.disponibilidad.domain.model.HorarioSemanal;
import prog2.servicioturnos.disponibilidad.domain.model.Ocupacion;
import prog2.servicioturnos.disponibilidad.domain.model.TurnoLibre;

class CalculadoraTurnosLibresTests {

    private static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");

    // Martes 3 de noviembre de 2026.
    private static final LocalDate MARTES = LocalDate.of(2026, 11, 3);

    // Lunes 2 de noviembre, 08:00 en Buenos Aires (11:00 UTC): el martes entero está en el futuro.
    private static final Instant LUNES_A_LA_MANIANA = Instant.parse("2026-11-02T11:00:00Z");

    private static final List<HorarioSemanal> MARTES_DE_9_A_12 =
            List.of(horario(DayOfWeek.TUESDAY, "09:00", "12:00", 30));

    @Test
    void conLaAgendaCompletaOfreceTodosLosTurnosDelDia() {
        List<TurnoLibre> libres =
                CalculadoraTurnosLibres.calcular(MARTES, MARTES_DE_9_A_12, List.of(), LUNES_A_LA_MANIANA, ZONA);

        assertThat(libres).extracting(TurnoLibre::horaInicio).containsExactly(
                hora("09:00"), hora("09:30"), hora("10:00"), hora("10:30"), hora("11:00"), hora("11:30"));
        assertThat(libres.getLast()).isEqualTo(new TurnoLibre(hora("11:30"), hora("12:00")));
    }

    @Test
    void sacaLosConfirmadosYLosRetenidosVigentes() {
        List<Ocupacion> ocupaciones = List.of(
                new Ocupacion(MARTES, hora("09:00"), EstadoOcupacion.CONFIRMADA, null),
                new Ocupacion(MARTES, hora("10:00"), EstadoOcupacion.RETENIDA, Instant.parse("2026-11-02T11:05:00Z")));

        List<TurnoLibre> libres =
                CalculadoraTurnosLibres.calcular(MARTES, MARTES_DE_9_A_12, ocupaciones, LUNES_A_LA_MANIANA, ZONA);

        assertThat(libres).extracting(TurnoLibre::horaInicio)
                .containsExactly(hora("09:30"), hora("10:30"), hora("11:00"), hora("11:30"));
    }

    @Test
    void unRetenidoVencidoCuentaComoLibre() {
        List<Ocupacion> ocupaciones = List.of(
                new Ocupacion(MARTES, hora("10:00"), EstadoOcupacion.RETENIDA, Instant.parse("2026-11-02T10:55:00Z")));

        List<TurnoLibre> libres =
                CalculadoraTurnosLibres.calcular(MARTES, MARTES_DE_9_A_12, ocupaciones, LUNES_A_LA_MANIANA, ZONA);

        assertThat(libres).extracting(TurnoLibre::horaInicio).contains(hora("10:00"));
    }

    @Test
    void conTodoOcupadoNoQuedanTurnosLibres() {
        List<Ocupacion> todas = List.of("09:00", "09:30", "10:00", "10:30", "11:00", "11:30").stream()
                .map(hora -> new Ocupacion(MARTES, hora(hora), EstadoOcupacion.CONFIRMADA, null))
                .toList();

        assertThat(CalculadoraTurnosLibres.calcular(MARTES, MARTES_DE_9_A_12, todas, LUNES_A_LA_MANIANA, ZONA))
                .isEmpty();
    }

    @Test
    void lasOcupacionesDeOtraFechaNoSeRestan() {
        List<Ocupacion> otroDia = List.of(
                new Ocupacion(MARTES.plusWeeks(1), hora("09:00"), EstadoOcupacion.CONFIRMADA, null));

        assertThat(CalculadoraTurnosLibres.calcular(MARTES, MARTES_DE_9_A_12, otroDia, LUNES_A_LA_MANIANA, ZONA))
                .hasSize(6);
    }

    @Test
    void siLaFechaEsHoySoloOfreceLosTurnosQueTodaviaNoEmpezaron() {
        // Martes 10:15 en Buenos Aires.
        Instant martesALas1015 = Instant.parse("2026-11-03T13:15:00Z");

        List<TurnoLibre> libres =
                CalculadoraTurnosLibres.calcular(MARTES, MARTES_DE_9_A_12, List.of(), martesALas1015, ZONA);

        assertThat(libres).extracting(TurnoLibre::horaInicio)
                .containsExactly(hora("10:30"), hora("11:00"), hora("11:30"));
    }

    @Test
    void unTurnoQueEmpiezaJustoAhoraYaNoSeOfrece() {
        Instant martesALas10 = Instant.parse("2026-11-03T13:00:00Z");

        assertThat(CalculadoraTurnosLibres.calcular(MARTES, MARTES_DE_9_A_12, List.of(), martesALas10, ZONA))
                .extracting(TurnoLibre::horaInicio).first().isEqualTo(hora("10:30"));
    }

    @Test
    void siElProfesionalNoAtiendeEseDiaNoHayTurnos() {
        List<HorarioSemanal> soloLunes = List.of(horario(DayOfWeek.MONDAY, "09:00", "12:00", 30));

        assertThat(CalculadoraTurnosLibres.atiendeEseDia(MARTES, soloLunes)).isFalse();
        assertThat(CalculadoraTurnosLibres.calcular(MARTES, soloLunes, List.of(), LUNES_A_LA_MANIANA, ZONA)).isEmpty();
    }

    @Test
    void unSobranteQueNoLlegaAUnTurnoEnteroNoSeOfrece() {
        List<HorarioSemanal> hasta1045 = List.of(horario(DayOfWeek.TUESDAY, "09:00", "10:45", 30));

        assertThat(CalculadoraTurnosLibres.calcular(MARTES, hasta1045, List.of(), LUNES_A_LA_MANIANA, ZONA))
                .extracting(TurnoLibre::horaFin)
                .containsExactly(hora("09:30"), hora("10:00"), hora("10:30"));
    }

    @Test
    void juntaVariosHorariosDelMismoDiaEnOrdenYSinRepetir() {
        List<HorarioSemanal> doble = List.of(
                horario(DayOfWeek.TUESDAY, "17:00", "18:00", 30),
                horario(DayOfWeek.TUESDAY, "09:00", "10:00", 30),
                horario(DayOfWeek.TUESDAY, "09:00", "10:00", 30));

        assertThat(CalculadoraTurnosLibres.calcular(MARTES, doble, List.of(), LUNES_A_LA_MANIANA, ZONA))
                .extracting(TurnoLibre::horaInicio)
                .containsExactly(hora("09:00"), hora("09:30"), hora("17:00"), hora("17:30"));
    }

    @Test
    void ignoraHorariosSinDuracionYNoPasaDeMedianoche() {
        List<HorarioSemanal> raros = List.of(
                horario(DayOfWeek.TUESDAY, "09:00", "12:00", 0),
                horario(DayOfWeek.TUESDAY, "23:00", "23:59", 30));

        assertThat(CalculadoraTurnosLibres.calcular(MARTES, raros, List.of(), LUNES_A_LA_MANIANA, ZONA))
                .containsExactly(new TurnoLibre(hora("23:00"), hora("23:30")));
    }

    @Test
    void unaFechaAnteriorAHoyYaPaso() {
        assertThat(CalculadoraTurnosLibres.esFechaPasada(MARTES.minusDays(2), LUNES_A_LA_MANIANA, ZONA)).isTrue();
        assertThat(CalculadoraTurnosLibres.esFechaPasada(MARTES.minusDays(1), LUNES_A_LA_MANIANA, ZONA)).isFalse();
        assertThat(CalculadoraTurnosLibres.esFechaPasada(MARTES, LUNES_A_LA_MANIANA, ZONA)).isFalse();
    }

    @Test
    void hoySeCalculaEnLaZonaHorariaDeLaAgenda() {
        // 01:00 UTC del martes todavía es lunes a la noche en Buenos Aires: el lunes no pasó.
        Instant lunesALaNoche = Instant.parse("2026-11-03T01:00:00Z");

        assertThat(CalculadoraTurnosLibres.esFechaPasada(MARTES.minusDays(1), lunesALaNoche, ZONA)).isFalse();
    }

    private static HorarioSemanal horario(DayOfWeek dia, String inicio, String fin, int duracion) {
        return new HorarioSemanal(dia, hora(inicio), hora(fin), duracion);
    }

    private static LocalTime hora(String texto) {
        return LocalTime.parse(texto);
    }
}

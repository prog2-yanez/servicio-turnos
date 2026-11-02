package prog2.servicioturnos.reservas.infrastructure.recuperacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import prog2.servicioturnos.config.TurnosConfig;
import prog2.servicioturnos.reservas.domain.model.ResultadoRecuperacion;
import prog2.servicioturnos.reservas.domain.ports.in.RecuperarProcesosUseCase;

class RecuperacionProgramadaTests {

    private final RecuperarProcesosUseCase recuperarProcesos = mock(RecuperarProcesosUseCase.class);

    private final ApplicationContextRunner contexto = new ApplicationContextRunner()
            .withUserConfiguration(TurnosConfig.class, RecuperacionProgramada.class)
            .withBean(RecuperarProcesosUseCase.class, () -> recuperarProcesos)
            .withPropertyValues("turnos.zona-horaria=UTC", "turnos.recuperacion.intervalo=50ms");

    // La primera ronda corre al arrancar y después se repite sola.
    @Test
    void correAlArrancarYDespuesPeriodicamente() {
        when(recuperarProcesos.recuperar()).thenReturn(new ResultadoRecuperacion(0, 0, 0));

        contexto.run(app -> verify(recuperarProcesos, timeout(2000).atLeast(3)).recuperar());
    }

    // El mismo formato que usa application.properties.
    @Test
    void aceptaElIntervaloEnMinutos() {
        when(recuperarProcesos.recuperar()).thenReturn(new ResultadoRecuperacion(0, 0, 0));

        contexto.withPropertyValues("turnos.recuperacion.intervalo=1m")
                .run(app -> assertThat(app).hasNotFailed().hasSingleBean(RecuperacionProgramada.class));
    }

    @Test
    void sePuedeApagar() {
        contexto.withPropertyValues("turnos.recuperacion.habilitada=false")
                .run(app -> assertThat(app).doesNotHaveBean(RecuperacionProgramada.class));
    }

    // Una ronda que falla no corta las siguientes.
    @Test
    void unaRondaQueFallaNoFrenaLasSiguientes() {
        when(recuperarProcesos.recuperar()).thenThrow(new IllegalStateException("sin base"))
                .thenReturn(new ResultadoRecuperacion(0, 0, 0));
        RecuperacionProgramada programada = new RecuperacionProgramada(recuperarProcesos);

        assertThatNoException().isThrownBy(programada::ejecutar);
        assertThatNoException().isThrownBy(programada::ejecutar);
        verify(recuperarProcesos, atLeast(2)).recuperar();
    }
}

package prog2.servicioturnos.reservas.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import prog2.servicioturnos.TestcontainersConfiguration;
import prog2.servicioturnos.reservas.domain.exception.ProcesoAbiertoException;
import prog2.servicioturnos.reservas.domain.model.EstadoProceso;
import prog2.servicioturnos.reservas.domain.model.ProcesoReserva;
import prog2.servicioturnos.reservas.infrastructure.persistence.mapper.ProcesoReservaMapper;
import prog2.servicioturnos.usuarios.domain.model.DatosRegistro;
import prog2.servicioturnos.usuarios.domain.model.Usuario;
import prog2.servicioturnos.usuarios.infrastructure.persistence.UsuarioPersistenceAdapter;
import prog2.servicioturnos.usuarios.infrastructure.persistence.mapper.UsuarioMapper;

// Contra PostgreSQL real: el índice único parcial es lo que garantiza un solo proceso abierto por usuario.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, ProcesoReservaMapper.class, ProcesoReservaPersistenceAdapter.class,
        UsuarioMapper.class, UsuarioPersistenceAdapter.class})
class ProcesoReservaPersistenciaTests {

    private static final Instant AHORA = Instant.parse("2026-11-02T12:00:00Z");
    private static final Instant VENCE = Instant.parse("2026-11-02T12:10:00Z");

    @Autowired
    private ProcesoReservaPersistenceAdapter procesos;

    @Autowired
    private UsuarioPersistenceAdapter usuarios;

    private long usuarioId;

    @BeforeEach
    void unUsuario() {
        DatosRegistro datos = new DatosRegistro("paciente-reservas", "secreta", "Ana", "Gomez",
                "paciente-reservas@mail.com", null, "es");
        usuarioId = usuarios.guardar(Usuario.registrar(datos, "hash", AHORA)).id();
    }

    @Test
    void guardaElProcesoConSuHoldYLoCuentaComoAbierto() {
        ProcesoReserva guardado = procesos.guardar(nuevo("09:00"));
        ProcesoReserva esperando = procesos.guardar(guardado.conHold("h-1", "rp-1", VENCE, AHORA)
                .esperandoTelefono(VENCE, AHORA));

        assertThat(guardado.id()).isNotNull();
        assertThat(esperando.id()).isEqualTo(guardado.id());
        assertThat(esperando.estado()).isEqualTo(EstadoProceso.ESPERANDO_TELEFONO);
        assertThat(esperando.holdId()).isEqualTo("h-1");
        assertThat(esperando.venceEn()).isEqualTo(VENCE);
        assertThat(procesos.tieneProcesoAbierto(usuarioId)).isTrue();
    }

    @Test
    void sinProcesosNoTieneNingunoAbierto() {
        assertThat(procesos.tieneProcesoAbierto(usuarioId)).isFalse();
    }

    @Test
    void unSegundoProcesoAbiertoDelMismoUsuarioSeRechazaAunqueElServicioNoLoHayaVisto() {
        procesos.guardar(nuevo("09:00"));

        assertThatThrownBy(() -> procesos.guardar(nuevo("09:30"))).isInstanceOf(ProcesoAbiertoException.class);
    }

    @Test
    void conLosProcesosAnterioresTerminadosPuedeIniciarOtro() {
        ProcesoReserva primero = procesos.guardar(nuevo("09:00"));
        procesos.guardar(primero.rechazado(AHORA));

        assertThat(procesos.tieneProcesoAbierto(usuarioId)).isFalse();
        assertThat(procesos.guardar(nuevo("09:30")).id()).isNotNull();
    }

    @Test
    void buscaElProcesoPorIdYElAbiertoDelUsuario() {
        ProcesoReserva terminado = procesos.guardar(nuevo("09:00"));
        procesos.guardar(terminado.rechazado(AHORA));
        ProcesoReserva abierto = procesos.guardar(nuevo("09:30"));

        assertThat(procesos.buscarPorId(terminado.id())).get()
                .extracting(ProcesoReserva::estado).isEqualTo(EstadoProceso.RECHAZADO);
        assertThat(procesos.buscarAbiertoDe(usuarioId)).get()
                .extracting(ProcesoReserva::id).isEqualTo(abierto.id());
        assertThat(procesos.buscarAbiertoDe(usuarioId + 1000)).isEmpty();
        assertThat(procesos.buscarPorId(-1)).isEmpty();
    }

    private ProcesoReserva nuevo(String hora) {
        return ProcesoReserva.iniciar(usuarioId, 15, LocalDate.of(2026, 11, 3), LocalTime.parse(hora), 7, AHORA);
    }
}

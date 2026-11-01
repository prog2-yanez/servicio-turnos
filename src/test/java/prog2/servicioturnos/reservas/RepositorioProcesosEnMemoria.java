package prog2.servicioturnos.reservas;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import prog2.servicioturnos.reservas.domain.model.ProcesoReserva;
import prog2.servicioturnos.reservas.domain.ports.out.ProcesoReservaRepository;

// Repositorio para las pruebas de los casos de uso: guarda en memoria y recuerda cada versión guardada, así se
// puede ver en qué orden cambió el proceso.
public final class RepositorioProcesosEnMemoria implements ProcesoReservaRepository {

    private final List<ProcesoReserva> historial = new ArrayList<>();
    private final AtomicLong ids = new AtomicLong(100);
    private boolean abierto;

    public List<ProcesoReserva> historial() {
        return historial;
    }

    public ProcesoReserva ultimo() {
        return historial.getLast();
    }

    // Simula que el usuario ya tiene otro proceso abierto.
    public void conProcesoAbierto() {
        abierto = true;
    }

    @Override
    public boolean tieneProcesoAbierto(long usuarioId) {
        return abierto;
    }

    @Override
    public Optional<ProcesoReserva> buscarPorId(long id) {
        return ultimaVersion(proceso -> proceso.id() != null && proceso.id() == id);
    }

    @Override
    public List<ProcesoReserva> listarDe(long usuarioId) {
        return historial.stream()
                .map(ProcesoReserva::id)
                .distinct()
                .map(id -> buscarPorId(id).orElseThrow())
                .filter(proceso -> proceso.esDe(usuarioId))
                .toList();
    }

    @Override
    public Optional<ProcesoReserva> buscarAbiertoDe(long usuarioId) {
        return historial.reversed().stream()
                .map(proceso -> buscarPorId(proceso.id()).orElseThrow())
                .filter(proceso -> proceso.esDe(usuarioId) && proceso.estaAbierto())
                .findFirst();
    }

    @Override
    public Optional<ProcesoReserva> buscarPorReservationProcessId(String reservationProcessId) {
        return ultimaVersion(proceso -> reservationProcessId.equals(proceso.reservationProcessId()));
    }

    @Override
    public ProcesoReserva guardar(ProcesoReserva proceso) {
        ProcesoReserva guardado = proceso.id() != null ? proceso : new ProcesoReserva(ids.incrementAndGet(),
                proceso.usuarioId(), proceso.profesionalId(), proceso.fecha(), proceso.horaInicio(),
                proceso.versionCatalogo(), proceso.estado(), proceso.holdId(), proceso.reservationProcessId(),
                proceso.requestEventId(), proceso.reservationId(), proceso.motivo(), proceso.venceEn(),
                proceso.creadoEn(), proceso.actualizadoEn());
        historial.add(guardado);
        return guardado;
    }

    private Optional<ProcesoReserva> ultimaVersion(Predicate<ProcesoReserva> condicion) {
        return historial.reversed().stream().filter(condicion).findFirst();
    }
}

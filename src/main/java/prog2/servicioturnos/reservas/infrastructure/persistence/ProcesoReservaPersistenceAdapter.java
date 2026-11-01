package prog2.servicioturnos.reservas.infrastructure.persistence;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import prog2.servicioturnos.reservas.domain.exception.ProcesoAbiertoException;
import prog2.servicioturnos.reservas.domain.model.EstadoProceso;
import prog2.servicioturnos.reservas.domain.model.ProcesoReserva;
import prog2.servicioturnos.reservas.domain.ports.out.ProcesoReservaRepository;
import prog2.servicioturnos.reservas.infrastructure.persistence.mapper.ProcesoReservaMapper;
import prog2.servicioturnos.reservas.infrastructure.persistence.repository.JpaProcesoReservaRepository;

@Component
public class ProcesoReservaPersistenceAdapter implements ProcesoReservaRepository {

    static final String INDICE_PROCESO_ABIERTO = "ux_proceso_abierto_por_usuario";

    private static final List<EstadoProceso> ABIERTOS =
            Arrays.stream(EstadoProceso.values()).filter(estado -> !estado.esFinal()).toList();

    private final JpaProcesoReservaRepository procesos;
    private final ProcesoReservaMapper mapper;

    public ProcesoReservaPersistenceAdapter(JpaProcesoReservaRepository procesos, ProcesoReservaMapper mapper) {
        this.procesos = procesos;
        this.mapper = mapper;
    }

    @Override
    public boolean tieneProcesoAbierto(long usuarioId) {
        return procesos.existsByUsuarioIdAndEstadoIn(usuarioId, ABIERTOS);
    }

    @Override
    public Optional<ProcesoReserva> buscarPorReservationProcessId(String reservationProcessId) {
        return procesos.findByReservationProcessId(reservationProcessId).map(mapper::aDominio);
    }

    // saveAndFlush hace que el índice único se controle acá y no al final de una transacción más grande.
    @Override
    public ProcesoReserva guardar(ProcesoReserva proceso) {
        try {
            return mapper.aDominio(procesos.saveAndFlush(mapper.aEntidad(proceso)));
        } catch (DataIntegrityViolationException e) {
            if (violaProcesoAbierto(e)) {
                throw new ProcesoAbiertoException();
            }
            throw e;
        }
    }

    private static boolean violaProcesoAbierto(DataIntegrityViolationException e) {
        Throwable causa = e;
        while (causa != null) {
            if (causa.getMessage() != null && causa.getMessage().contains(INDICE_PROCESO_ABIERTO)) {
                return true;
            }
            causa = causa.getCause();
        }
        return false;
    }
}

package prog2.servicioturnos.reservas.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import prog2.servicioturnos.reservas.domain.model.EstadoProceso;
import prog2.servicioturnos.reservas.infrastructure.persistence.entity.ProcesoReservaEntity;

public interface JpaProcesoReservaRepository extends JpaRepository<ProcesoReservaEntity, Long> {

    boolean existsByUsuarioIdAndEstadoIn(long usuarioId, Collection<EstadoProceso> estados);

    Optional<ProcesoReservaEntity> findByReservationProcessId(String reservationProcessId);

    Optional<ProcesoReservaEntity> findFirstByUsuarioIdAndEstadoIn(long usuarioId, Collection<EstadoProceso> estados);
}

package prog2.servicioturnos.reservas.infrastructure.persistence.mapper;

import org.springframework.stereotype.Component;
import prog2.servicioturnos.reservas.domain.model.ProcesoReserva;
import prog2.servicioturnos.reservas.infrastructure.persistence.entity.ProcesoReservaEntity;

@Component
public class ProcesoReservaMapper {

    public ProcesoReservaEntity aEntidad(ProcesoReserva proceso) {
        ProcesoReservaEntity entidad = new ProcesoReservaEntity();
        entidad.setId(proceso.id());
        entidad.setUsuarioId(proceso.usuarioId());
        entidad.setProfesionalId(proceso.profesionalId());
        entidad.setFecha(proceso.fecha());
        entidad.setHoraInicio(proceso.horaInicio());
        entidad.setVersionCatalogo(proceso.versionCatalogo());
        entidad.setEstado(proceso.estado());
        entidad.setHoldId(proceso.holdId());
        entidad.setReservationProcessId(proceso.reservationProcessId());
        entidad.setRequestEventId(proceso.requestEventId());
        entidad.setReservationId(proceso.reservationId());
        entidad.setMotivo(proceso.motivo());
        entidad.setVenceEn(proceso.venceEn());
        entidad.setCreadoEn(proceso.creadoEn());
        entidad.setActualizadoEn(proceso.actualizadoEn());
        return entidad;
    }

    public ProcesoReserva aDominio(ProcesoReservaEntity entidad) {
        return new ProcesoReserva(entidad.getId(), entidad.getUsuarioId(), entidad.getProfesionalId(),
                entidad.getFecha(), entidad.getHoraInicio(), entidad.getVersionCatalogo(), entidad.getEstado(),
                entidad.getHoldId(), entidad.getReservationProcessId(), entidad.getRequestEventId(),
                entidad.getReservationId(), entidad.getMotivo(), entidad.getVenceEn(), entidad.getCreadoEn(),
                entidad.getActualizadoEn());
    }
}

package prog2.servicioturnos.reservas.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import prog2.servicioturnos.reservas.domain.model.EstadoProceso;

@Entity
@Table(name = "proceso_reserva")
@Getter
@Setter
@NoArgsConstructor
public class ProcesoReservaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private long usuarioId;

    @Column(name = "profesional_id", nullable = false)
    private long profesionalId;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "version_catalogo", nullable = false)
    private long versionCatalogo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoProceso estado;

    @Column(name = "hold_id", length = 100)
    private String holdId;

    @Column(name = "reservation_process_id", length = 100, unique = true)
    private String reservationProcessId;

    @Column(name = "request_event_id", length = 100)
    private String requestEventId;

    @Column(name = "reservation_id", length = 100)
    private String reservationId;

    @Column(length = 500)
    private String motivo;

    @Column(name = "vence_en")
    private Instant venceEn;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;
}

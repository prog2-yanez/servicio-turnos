package prog2.servicioturnos.reservas.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "evento_procesado")
@Getter
@Setter
@NoArgsConstructor
public class EventoProcesadoEntity {

    @Id
    @Column(name = "event_id", length = 100)
    private String eventId;

    @Column(nullable = false, length = 100)
    private String tipo;

    @Column(name = "procesado_en", nullable = false)
    private Instant procesadoEn;
}

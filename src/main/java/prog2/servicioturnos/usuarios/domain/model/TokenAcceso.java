package prog2.servicioturnos.usuarios.domain.model;

import java.time.Instant;

// JWT firmado que la app manda en cada pedido, con el momento en que deja de valer.
public record TokenAcceso(String valor, Instant venceEn) {
}

package prog2.servicioturnos.reservas.domain.model;

import java.util.List;

// desactualizado = true cuando la cátedra no respondió y la lista sale solo de lo que tiene guardado turnos.
public record ReservasPropias(List<ReservaPropia> reservas, boolean desactualizado) {

    public ReservasPropias {
        reservas = List.copyOf(reservas);
    }
}

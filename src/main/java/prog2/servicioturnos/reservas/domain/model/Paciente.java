package prog2.servicioturnos.reservas.domain.model;

// Quién reserva, con los datos que pide la cátedra. La cátedra solo conoce la cuenta técnica: para distinguir a
// nuestros usuarios se le manda como externalPatientId el id estable del usuario en turnos.
public record Paciente(long usuarioId, String nombre, String apellido) {

    // La cátedra pide nombre y apellido de entre 2 y 100 caracteres (contrato, sección 10).
    static final int LARGO_MINIMO = 2;
    static final int LARGO_MAXIMO = 100;

    public String externalPatientId() {
        return String.valueOf(usuarioId);
    }

    public boolean tieneNombreCompleto() {
        return esValido(nombre) && esValido(apellido);
    }

    private static boolean esValido(String texto) {
        return texto != null && texto.strip().length() >= LARGO_MINIMO && texto.strip().length() <= LARGO_MAXIMO;
    }
}

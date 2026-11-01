package prog2.servicioturnos.reservas.domain.model;

import java.util.regex.Pattern;
import prog2.servicioturnos.reservas.domain.exception.TelefonoInvalidoException;

// Teléfono del paciente con el formato que exige la cátedra (contrato, sección 15.5): se le sacan espacios,
// guiones, paréntesis y puntos, y tiene que quedar un + opcional seguido de 7 a 15 dígitos.
public record Telefono(String numero) {

    private static final Pattern FORMATO = Pattern.compile("^\\+?[0-9]{7,15}$");
    private static final Pattern SEPARADORES = Pattern.compile("[\\s\\-().]");

    public static Telefono normalizar(String escrito) {
        String numero = escrito == null ? "" : SEPARADORES.matcher(escrito).replaceAll("");
        if (!FORMATO.matcher(numero).matches()) {
            throw new TelefonoInvalidoException();
        }
        return new Telefono(numero);
    }
}

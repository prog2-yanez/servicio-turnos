package prog2.servicioturnos.reservas.domain.exception;

// La cátedra no contestó (timeout, sin conexión o error 5xx): pudo haber hecho el cambio o no. No es un error para
// el usuario ni se reintenta a ciegas: el proceso queda INCIERTO y lo resuelve la recuperación.
public class ResultadoInciertoException extends RuntimeException {

    public ResultadoInciertoException(String motivo) {
        super(motivo);
    }
}

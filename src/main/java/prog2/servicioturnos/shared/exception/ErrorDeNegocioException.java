package prog2.servicioturnos.shared.exception;

// Base de los errores que el servicio informa a propósito; el código arma el type del ProblemDetail.
public abstract class ErrorDeNegocioException extends RuntimeException {

    private final String codigo;

    protected ErrorDeNegocioException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }
}

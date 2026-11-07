package prog2.servicioturnos.shared.exception;

// Lo que se busca no existe o no pertenece al usuario: en los dos casos se responde 404.
public abstract class RecursoNoEncontradoException extends ErrorDeNegocioException {

    protected RecursoNoEncontradoException(String codigo, String mensaje) {
        super(codigo, mensaje);
    }
}

package prog2.servicioturnos.shared.web;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import prog2.servicioturnos.shared.exception.ConflictoException;
import prog2.servicioturnos.shared.exception.RecursoNoEncontradoException;

// Traduce las excepciones a ProblemDetail (RFC 7807), con títulos y detalles en español.
@RestControllerAdvice
public class ManejadorErroresApi extends ResponseEntityExceptionHandler {

    static final String PREFIJO_TIPO = "urn:turnos:error:";

    private static final Logger log = LoggerFactory.getLogger(ManejadorErroresApi.class);

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ProblemDetail recursoNoEncontrado(RecursoNoEncontradoException e, HttpServletRequest pedido) {
        return problema(HttpStatus.NOT_FOUND, e.codigo(), "Recurso no encontrado", e.getMessage(), pedido);
    }

    @ExceptionHandler(ConflictoException.class)
    public ProblemDetail conflicto(ConflictoException e, HttpServletRequest pedido) {
        return problema(HttpStatus.CONFLICT, e.codigo(), "Conflicto", e.getMessage(), pedido);
    }

    // La traza queda solo en el log; al cliente no se le muestra nada interno.
    @ExceptionHandler(Exception.class)
    public ProblemDetail errorInterno(Exception e, HttpServletRequest pedido) {
        log.error("Error inesperado en {}", pedido.getRequestURI(), e);
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, "error-interno", "Error interno",
                "Ocurrió un error inesperado", pedido);
    }

    // Cuerpo JSON con campos que no cumplen sus validaciones: nombra cada campo que falló.
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode estado, WebRequest pedido) {
        String detalle = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        ProblemDetail problema = conTipo(estado, "datos-invalidos", "Datos inválidos", "Datos inválidos. " + detalle);
        return handleExceptionInternal(ex, problema, headers, estado, pedido);
    }

    // Parámetros de la URL que no cumplen sus validaciones (por ejemplo @Max).
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode estado, WebRequest pedido) {
        String detalle = ex.getParameterValidationResults().stream()
                .flatMap(resultado -> resultado.getResolvableErrors().stream()
                        .map(error -> resultado.getMethodParameter().getParameterName() + ": "
                                + error.getDefaultMessage()))
                .collect(Collectors.joining("; "));
        ProblemDetail problema = conTipo(estado, "parametro-invalido", "Parámetro inválido",
                "Parámetros inválidos. " + detalle);
        return handleExceptionInternal(ex, problema, headers, estado, pedido);
    }

    // Un valor que no se puede convertir al tipo esperado, como un número con letras.
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException ex, HttpHeaders headers, HttpStatusCode estado, WebRequest pedido) {
        String parametro = ex instanceof MethodArgumentTypeMismatchException conNombre
                ? conNombre.getName()
                : ex.getPropertyName();
        ProblemDetail problema = conTipo(estado, "parametro-invalido", "Parámetro inválido",
                parametro + ": el valor '" + ex.getValue() + "' no es válido");
        return handleExceptionInternal(ex, problema, headers, estado, pedido);
    }

    // JSON mal escrito o con tipos que no corresponden; no se repite el mensaje técnico del parser.
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode estado, WebRequest pedido) {
        ProblemDetail problema = conTipo(estado, "parametro-invalido", "Cuerpo inválido",
                "El cuerpo del pedido no es un JSON válido");
        return handleExceptionInternal(ex, problema, headers, estado, pedido);
    }

    // Completa type, title e instance en los errores que Spring MVC ya convierte a ProblemDetail.
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode estado, WebRequest pedido) {
        ResponseEntity<Object> respuesta = super.handleExceptionInternal(ex, body, headers, estado, pedido);
        if (respuesta != null && respuesta.getBody() instanceof ProblemDetail problema) {
            if (problema.getType() == null || "about:blank".equals(problema.getType().toString())) {
                problema.setType(URI.create(PREFIJO_TIPO + codigoSegunEstado(estado)));
                problema.setTitle(tituloSegunEstado(estado));
            }
            if (pedido instanceof ServletWebRequest servlet) {
                problema.setInstance(URI.create(servlet.getRequest().getRequestURI()));
            }
        }
        return respuesta;
    }

    private static String codigoSegunEstado(HttpStatusCode estado) {
        return switch (estado.value()) {
            case 400 -> "parametro-invalido";
            case 404 -> "ruta-no-encontrada";
            case 405 -> "metodo-no-permitido";
            case 415 -> "formato-no-soportado";
            default -> "error-http";
        };
    }

    private static String tituloSegunEstado(HttpStatusCode estado) {
        return switch (estado.value()) {
            case 400 -> "Parámetro inválido";
            case 404 -> "Ruta no encontrada";
            case 405 -> "Método no permitido";
            case 415 -> "Formato no soportado";
            default -> "Error en el pedido";
        };
    }

    private static ProblemDetail conTipo(HttpStatusCode estado, String codigo, String titulo, String detalle) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
        problema.setType(URI.create(PREFIJO_TIPO + codigo));
        problema.setTitle(titulo);
        return problema;
    }

    private static ProblemDetail problema(
            HttpStatus estado, String codigo, String titulo, String detalle, HttpServletRequest pedido) {
        ProblemDetail problema = conTipo(estado, codigo, titulo, detalle);
        problema.setInstance(URI.create(pedido.getRequestURI()));
        return problema;
    }
}

package prog2.servicioturnos.shared.web;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Map;
import java.util.Objects;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import prog2.servicioturnos.shared.exception.ConflictoException;
import prog2.servicioturnos.shared.exception.NoAutenticadoException;
import prog2.servicioturnos.shared.exception.RecursoNoEncontradoException;

// Traduce las excepciones a ProblemDetail (RFC 7807), con títulos y detalles en español.
@RestControllerAdvice
public class ManejadorErroresApi extends ResponseEntityExceptionHandler {

    static final String PREFIJO_TIPO = "urn:turnos:error:";

    private static final URI SIN_TIPO = URI.create("about:blank");

    // Tipo y título de los errores que Spring MVC genera solo, según el status.
    private static final Map<Integer, TipoDeError> TIPOS_POR_ESTADO = Map.of(
            400, new TipoDeError("parametro-invalido", "Parámetro inválido"),
            404, new TipoDeError("ruta-no-encontrada", "Ruta no encontrada"),
            405, new TipoDeError("metodo-no-permitido", "Método no permitido"),
            415, new TipoDeError("formato-no-soportado", "Formato no soportado"));

    private static final TipoDeError TIPO_GENERICO = new TipoDeError("error-http", "Error en el pedido");

    private static final Logger log = LoggerFactory.getLogger(ManejadorErroresApi.class);

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ProblemDetail recursoNoEncontrado(RecursoNoEncontradoException e, HttpServletRequest pedido) {
        return problema(HttpStatus.NOT_FOUND, e.codigo(), "Recurso no encontrado", e.getMessage(), pedido);
    }

    @ExceptionHandler(NoAutenticadoException.class)
    public ProblemDetail noAutenticado(NoAutenticadoException e, HttpServletRequest pedido) {
        return problema(HttpStatus.UNAUTHORIZED, e.codigo(), "No autenticado", e.getMessage(), pedido);
    }

    // Sin token o con un esquema que no es Bearer.
    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail sinAutenticar(AuthenticationException e, HttpServletRequest pedido) {
        return problema(HttpStatus.UNAUTHORIZED, "no-autenticado", "No autenticado",
                "Hace falta iniciar sesión para usar este recurso", pedido);
    }

    // Token con firma incorrecta, vencido o de otro emisor: la app tiene que volver a iniciar sesión.
    @ExceptionHandler(InvalidBearerTokenException.class)
    public ProblemDetail tokenInvalido(InvalidBearerTokenException e, HttpServletRequest pedido) {
        return problema(HttpStatus.UNAUTHORIZED, "token-invalido", "Token inválido",
                "El token es inválido o venció; hay que volver a iniciar sesión", pedido);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail sinPermiso(AccessDeniedException e, HttpServletRequest pedido) {
        return problema(HttpStatus.FORBIDDEN, "sin-permiso", "Sin permiso",
                "El usuario no tiene permiso para usar este recurso", pedido);
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
        ProblemDetail problema = conTipo(estado, "parametro-invalido", "Parámetro inválido",
                ex.getPropertyName() + ": el valor '" + ex.getValue() + "' no es válido");
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
            if (SIN_TIPO.equals(Objects.requireNonNullElse(problema.getType(), SIN_TIPO))) {
                TipoDeError tipo = TIPOS_POR_ESTADO.getOrDefault(estado.value(), TIPO_GENERICO);
                problema.setType(URI.create(PREFIJO_TIPO + tipo.codigo()));
                problema.setTitle(tipo.titulo());
            }
            if (pedido instanceof ServletWebRequest servlet) {
                problema.setInstance(URI.create(servlet.getRequest().getRequestURI()));
            }
        }
        return respuesta;
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

    private record TipoDeError(String codigo, String titulo) {
    }
}

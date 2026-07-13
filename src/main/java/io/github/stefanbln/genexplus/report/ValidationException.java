package io.github.stefanbln.genexplus.report;

/**
 * Indicates invalid user input or report parameters.
 *
 * <p>Thrown when values fail semantic validation before report generation begins.
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}

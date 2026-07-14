package io.github.stefanbln.genexplus.report;

import java.io.Serial;

/**
 * Indicates invalid user input or report parameters.
 *
 * <p>Thrown when values fail semantic validation before report generation begins.
 */
public class ValidationException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1_125_008_793_485_507_474L;

    public ValidationException(String message) {
        super(message);
    }

}

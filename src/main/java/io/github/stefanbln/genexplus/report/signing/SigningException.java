package io.github.stefanbln.genexplus.report.signing;

import java.io.Serial;

/**
 * Indicates PDF signing failed due to configuration, keystore, or cryptographic errors.
 */
public final class SigningException extends Exception {

    @Serial
    private static final long serialVersionUID = -6_008_364_709_740_800_836L;

    public SigningException(String message) {
        super(message);
    }

    public SigningException(String message, Throwable cause) {
        super(message, cause);
    }
}

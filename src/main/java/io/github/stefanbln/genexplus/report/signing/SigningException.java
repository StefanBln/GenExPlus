package io.github.stefanbln.genexplus.report.signing;

/**
 * Indicates PDF signing failed due to configuration, keystore, or cryptographic errors.
 */
public final class SigningException extends Exception {

    public SigningException(String message) {
        super(message);
    }

    public SigningException(String message, Throwable cause) {
        super(message, cause);
    }
}

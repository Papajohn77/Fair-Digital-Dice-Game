package gr.aueb.casino.exception.custom;

public class InvalidNonceException extends RuntimeException {
    public InvalidNonceException(String message) {
        super(message);
    }
}

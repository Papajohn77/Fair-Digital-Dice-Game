package gr.aueb.casino.exception.custom;

public class BreachedPasswordException extends RuntimeException {
    public BreachedPasswordException(String message) {
        super(message);
    }
}

package gr.aueb.casino.exception.custom;

public class GameAccessDeniedException extends RuntimeException {
    public GameAccessDeniedException(String message) {
        super(message);
    }
}

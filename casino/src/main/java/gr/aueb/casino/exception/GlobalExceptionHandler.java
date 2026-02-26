package gr.aueb.casino.exception;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.ModelAndView;

import gr.aueb.casino.exception.custom.GameAccessDeniedException;
import gr.aueb.casino.exception.custom.GameNotFoundException;
import gr.aueb.casino.exception.custom.InvalidNonceException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ResponseBody
    @ExceptionHandler(GameNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleGameNotFound(GameNotFoundException ex) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", ex.getMessage()));
    }

    @ResponseBody
    @ExceptionHandler(GameAccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleGameAccessDenied(GameAccessDeniedException ex) {
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(Map.of("error", ex.getMessage()));
    }

    @ResponseBody
    @ExceptionHandler(InvalidNonceException.class)
    public ResponseEntity<Map<String, String>> handleInvalidNonce(InvalidNonceException ex) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleAllUncaughtException(Exception ex, WebRequest request) {
        log.error("Uncaught exception: {}", ex.getMessage(), ex);

        ModelAndView mav = new ModelAndView("error");
        mav.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        mav.addObject("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        mav.addObject("error", "Internal Server Error");
        mav.addObject("message", "An unexpected error occurred.");
        return mav;
    }
}

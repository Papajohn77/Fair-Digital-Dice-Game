package gr.aueb.casino.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/game")
public class GameController {

    @GetMapping
    public String gamePage() {
        return "game";
    }

    @GetMapping("/test-error")
    public String testError() {
        throw new RuntimeException("This is a test exception to verify GlobalExceptionHandler");
    }
}

package gr.aueb.casino.api;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import gr.aueb.casino.api.schemas.request.GuessRequest;
import gr.aueb.casino.api.schemas.request.InitiateGameRequest;
import gr.aueb.casino.api.schemas.response.GuessResponse;
import gr.aueb.casino.api.schemas.response.InitiateGameResponse;
import gr.aueb.casino.security.UserDetailsAdapter;
import gr.aueb.casino.service.GameService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/game")
@RequiredArgsConstructor
public class GameController {
    private final GameService gameService;

    @GetMapping
    public String gamePage() {
        return "game";
    }

    @PostMapping
    @ResponseBody
    public InitiateGameResponse initiateGame(
        @Valid @RequestBody InitiateGameRequest request,
        @AuthenticationPrincipal UserDetailsAdapter userDetails
    ) {
        return gameService.initiateGame(userDetails.getId(), request.clientNonce());
    }

    @PostMapping("/{id}/guess")
    @ResponseBody
    public GuessResponse submitGuess(
        @PathVariable Long id,
        @Valid @RequestBody GuessRequest request,
        @AuthenticationPrincipal UserDetailsAdapter userDetails
    ) {
        return gameService.submitGuess(id, request.clientRoll(), userDetails.getId());
    }
}

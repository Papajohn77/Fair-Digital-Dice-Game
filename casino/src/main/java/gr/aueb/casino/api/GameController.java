package gr.aueb.casino.api;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import gr.aueb.casino.api.schemas.request.InitiateGameRequest;
import gr.aueb.casino.api.schemas.request.RevealRequest;
import gr.aueb.casino.api.schemas.response.InitiateGameResponse;
import gr.aueb.casino.api.schemas.response.RevealResponse;
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
    public String gamePage(@AuthenticationPrincipal UserDetailsAdapter userDetails, Model model) {
        model.addAttribute("recentGames", gameService.getRecentGames(userDetails.getId()));
        return "game";
    }

    @PostMapping
    @ResponseBody
    public InitiateGameResponse initiateGame(
        @Valid @RequestBody InitiateGameRequest request,
        @AuthenticationPrincipal UserDetailsAdapter userDetails
    ) {
        return gameService.initiateGame(userDetails.getId(), request.clientNonceHash());
    }

    @PostMapping("/{id}/reveal")
    @ResponseBody
    public RevealResponse revealNonces(
        @PathVariable Long id,
        @Valid @RequestBody RevealRequest request,
        @AuthenticationPrincipal UserDetailsAdapter userDetails
    ) {
        return gameService.revealNonces(id, request.clientNonce(), userDetails.getId());
    }
}

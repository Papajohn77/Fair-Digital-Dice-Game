package gr.aueb.casino.api.schemas.response;

public record GuessResponse(
    String gameOutcome,
    short serverRoll,
    String serverNonce
) {}

package gr.aueb.casino.api.schemas.response;

public record RevealResponse(
    String gameOutcome,
    short serverRoll,
    short clientRoll,
    String serverNonce
) {}

package gr.aueb.casino.api.schemas.response;

public record InitiateGameResponse(
    Long gameId,
    String serverNonceHash
) {}

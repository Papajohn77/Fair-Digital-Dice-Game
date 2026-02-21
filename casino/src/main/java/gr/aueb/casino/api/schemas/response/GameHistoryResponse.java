package gr.aueb.casino.api.schemas.response;

import java.time.ZonedDateTime;

public record GameHistoryResponse(
    short serverRoll,
    short clientRoll,
    String outcome,
    ZonedDateTime completedAt
) {}

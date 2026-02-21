package gr.aueb.casino.api.schemas.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record GuessRequest(

    @NotNull(message = "Client roll is required.")
    @Min(value = 1, message = "Client roll must be between 1 and 6.")
    @Max(value = 6, message = "Client roll must be between 1 and 6.")
    Short clientRoll
) {}

package gr.aueb.casino.api.schemas.request;

import jakarta.validation.constraints.Pattern;

public record RevealRequest(

    @Pattern(
        regexp = "^[a-f0-9]{64}$",
        message = "Client nonce must be a 64-character hexadecimal string."
    )
    String clientNonce
) {}

package gr.aueb.casino.api.schemas.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

    @NotBlank(message = "First name is required.")
    @Size(max = 100, message = "First name must not exceed 100 characters.")
    String firstName,

    @NotBlank(message = "Last name is required.")
    @Size(max = 100, message = "Last name must not exceed 100 characters.")
    String lastName,

    @NotBlank(message = "Email is required.")
    @Email(message = "Invalid email address.")
    String email,

    @Pattern(
        regexp = "^(?=.*[A-Z])(?=.*[!@#$%^&+=.]).{12,}$",
        message = "Password must be at least 12 characters long and contain at least one uppercase letter and one special character."
    )
    String password,

    String confirmPassword
) {}

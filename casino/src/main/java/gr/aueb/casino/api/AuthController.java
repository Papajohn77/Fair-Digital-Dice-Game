package gr.aueb.casino.api;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import gr.aueb.casino.api.schemas.request.RegisterRequest;
import gr.aueb.casino.exception.custom.AlreadyExistsException;
import gr.aueb.casino.exception.custom.BreachedPasswordException;
import gr.aueb.casino.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;

    @GetMapping("/login")
    public String loginPage(
        @RequestParam(required = false) String error,
        @RequestParam(required = false) String logout,
        @RequestParam(required = false) String registered,
        Model model
    ) {
        if (error != null) model.addAttribute("error", "Invalid username or password.");
        if (logout != null) model.addAttribute("info", "You have been logged out.");
        if (registered != null) model.addAttribute("info", "Registration successful. Please log in.");
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest("", "", "", "", ""));
        return "register";
    }

    @PostMapping("/register")
    public String register(
        @Valid @ModelAttribute RegisterRequest registerRequest,
        BindingResult bindingResult,
        Model model
    ) {
        if (!registerRequest.password().equals(registerRequest.confirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "error.passwordMatch", "Passwords do not match.");
        }

        if (bindingResult.hasErrors()) {
            return "register";
        }

        try {
            userService.register(registerRequest);
        } catch (AlreadyExistsException | BreachedPasswordException e) {
            model.addAttribute("error", e.getMessage());
            return "register";
        }

        return "redirect:/auth/login?registered=true";
    }
}

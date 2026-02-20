package gr.aueb.casino.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.password.HaveIBeenPwnedRestApiPasswordChecker;
import org.springframework.stereotype.Service;

import gr.aueb.casino.api.schemas.request.RegisterRequest;
import gr.aueb.casino.domain.User;
import gr.aueb.casino.exception.custom.AlreadyExistsException;
import gr.aueb.casino.exception.custom.BreachedPasswordException;
import gr.aueb.casino.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final HaveIBeenPwnedRestApiPasswordChecker compromisedPasswordChecker;

    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new AlreadyExistsException("Email '" + request.email() + "' is already registered.");
        }

        checkForBreachedPassword(request.password());

        User user = new User(
            request.firstName(),
            request.lastName(),
            request.email(),
            passwordEncoder.encode(request.password())
        );
        userRepository.save(user);
    }

    private void checkForBreachedPassword(String password) {
        try {
            var result = compromisedPasswordChecker.check(password);
            if (result.isCompromised()) {
                throw new BreachedPasswordException("This password has been found in a known data breach. Please choose a different password.");
            }
        } catch (BreachedPasswordException e) {
            throw e;
        } catch (Exception e) {
            log.warn("HaveIBeenPwned service is unavailable, skipping breach check: {}", e.getMessage());
        }
    }
}

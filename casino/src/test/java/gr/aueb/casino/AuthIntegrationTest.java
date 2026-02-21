package gr.aueb.casino;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.password.CompromisedPasswordChecker;
import org.springframework.security.authentication.password.CompromisedPasswordDecision;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@Testcontainers
@SpringBootTest
class AuthIntegrationTest {
    private static final String TEST_PASSWORD = "V3ryStr0ng!T3stP@ssw0rd2024";

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        @Primary
        public CompromisedPasswordChecker compromisedPasswordChecker() {
            return password -> new CompromisedPasswordDecision(false);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:18"))
        .withDatabaseName("GDPR")
        .withUsername("casino")
        .withPassword("mysecretpassword");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:8.6"))
        .withExposedPorts(6379);

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Test
    void successfulRegistration() throws Exception {
        mockMvc.perform(
            post("/auth/register")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .param("firstName", "John")
            .param("lastName", "Doe")
            .param("email", "john@example.com")
            .param("password", TEST_PASSWORD)
            .param("confirmPassword", TEST_PASSWORD)
            .with(csrf())
        )
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/auth/login?registered=true"));
    }

    @Test
    void registrationFailsWhenUsernameAlreadyExists() throws Exception {
        String email = "duplicate@example.com";
        registerUser(email, TEST_PASSWORD);

        mockMvc.perform(
            post("/auth/register")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .param("firstName", "Jane")
            .param("lastName", "Doe")
            .param("email", email)
            .param("password", TEST_PASSWORD)
            .param("confirmPassword", TEST_PASSWORD)
            .with(csrf())
        )
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("already registered")));
    }

    @Test
    void registrationFailsOnPasswordMismatch() throws Exception {
        mockMvc.perform(
            post("/auth/register")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .param("firstName", "Alice")
            .param("lastName", "Smith")
            .param("email", "alice@example.com")
            .param("password", TEST_PASSWORD)
            .param("confirmPassword", "DifferentPass1!")
            .with(csrf())
        )
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Passwords do not match")));
    }

    @Test
    void registrationFailsWithWeakPassword() throws Exception {
        mockMvc.perform(
            post("/auth/register")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .param("firstName", "Bob")
            .param("lastName", "Smith")
            .param("email", "bob@example.com")
            .param("password", "weakpassword")
            .param("confirmPassword", "weakpassword")
            .with(csrf())
        )
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Password must be at least 12 characters long and contain at least one uppercase letter and one special character.")));
    }

    @Test
    void successfulLoginRedirectsToGame() throws Exception {
        String email = "login@example.com";
        registerUser(email, TEST_PASSWORD);

        mockMvc.perform(
            post("/auth/login")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .param("email", email)
            .param("password", TEST_PASSWORD)
            .with(csrf())
        )
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/game"));
    }

    @Test
    void loginWithWrongPasswordRedirectsToError() throws Exception {
        String email = "wrongpass@example.com";
        registerUser(email, TEST_PASSWORD);

        mockMvc.perform(
            post("/auth/login")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .param("email", email)
            .param("password", "WrongPass999!")
            .with(csrf())
        )
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/auth/login?error=true"));
    }

    @Test
    void unauthenticatedUserIsRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/game"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/auth/login"));
    }

    @Test
    void successfulLogoutInvalidatesSession() throws Exception {
        String email = "logout@example.com";
        registerUser(email, TEST_PASSWORD);

        MockHttpSession session = performLoginAndGetSession(email, TEST_PASSWORD);

        mockMvc.perform(
            post("/auth/logout")
            .session(session)
            .with(csrf())
        )
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/auth/login?logout=true"));

        mockMvc.perform(get("/game").session(session))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/auth/login"));
    }

    private void registerUser(String email, String password) throws Exception {
        mockMvc.perform(
            post("/auth/register")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .param("firstName", "Test")
            .param("lastName", "User")
            .param("email", email)
            .param("password", password)
            .param("confirmPassword", password)
            .with(csrf())
        );
    }

    private MockHttpSession performLoginAndGetSession(String email, String password) throws Exception {
        return (MockHttpSession) mockMvc.perform(
            post("/auth/login")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .param("email", email)
            .param("password", password)
            .with(csrf())
        )
        .andReturn()
        .getRequest()
        .getSession();
    }
}

package gr.aueb.casino;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.password.CompromisedPasswordChecker;
import org.springframework.security.authentication.password.CompromisedPasswordDecision;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import gr.aueb.casino.domain.User;
import gr.aueb.casino.persistence.GameRepository;
import gr.aueb.casino.persistence.UserRepository;
import gr.aueb.casino.security.UserDetailsAdapter;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@Testcontainers
@SpringBootTest
class GameIntegrationTest {
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

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameRepository gameRepository;

    @AfterEach
    void tearDown() {
        gameRepository.deleteAll();
        userRepository.deleteAll();
    }

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
    void successfulGameFlow() throws Exception {
        User user = createUser("player@example.com", TEST_PASSWORD);

        String clientNonce = "a".repeat(64);
        String clientNonceHash = sha256Hex(clientNonce);

        MvcResult initiateResult = mockMvc.perform(
            post("/game")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"clientNonceHash\":\"" + clientNonceHash + "\"}")
            .with(user(new UserDetailsAdapter(user)))
            .with(csrf())
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.gameId", notNullValue()))
        .andExpect(jsonPath("$.serverNonceHash", matchesPattern("^[a-f0-9]{64}$")))
        .andReturn();

        JsonNode initiateResponse = objectMapper.readTree(initiateResult.getResponse().getContentAsString());
        long gameId = initiateResponse.get("gameId").asLong();

        mockMvc.perform(
            post("/game/" + gameId + "/reveal")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"clientNonce\":\"" + clientNonce + "\"}")
            .with(user(new UserDetailsAdapter(user)))
            .with(csrf())
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.gameOutcome").exists())
        .andExpect(jsonPath("$.serverRoll", allOf(greaterThanOrEqualTo(1), lessThanOrEqualTo(6))))
        .andExpect(jsonPath("$.clientRoll", allOf(greaterThanOrEqualTo(1), lessThanOrEqualTo(6))))
        .andExpect(jsonPath("$.serverNonce", matchesPattern("^[a-f0-9]{64}$")));
    }

    @Test
    void userCannotAccessOtherUsersGame() throws Exception {
        User user1 = createUser("user1@example.com", TEST_PASSWORD);
        User user2 = createUser("user2@example.com", TEST_PASSWORD);

        String clientNonce = "c".repeat(64);
        String clientNonceHash = sha256Hex(clientNonce);

        MvcResult initiateResult = mockMvc.perform(
            post("/game")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"clientNonceHash\":\"" + clientNonceHash + "\"}")
            .with(user(new UserDetailsAdapter(user1)))
            .with(csrf())
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.gameId", notNullValue()))
        .andExpect(jsonPath("$.serverNonceHash", matchesPattern("^[a-f0-9]{64}$")))
        .andReturn();

        JsonNode initiateResponse = objectMapper.readTree(initiateResult.getResponse().getContentAsString());
        long gameId = initiateResponse.get("gameId").asLong();

        mockMvc.perform(
            post("/game/" + gameId + "/reveal")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"clientNonce\":\"" + clientNonce + "\"}")
            .with(user(new UserDetailsAdapter(user2)))
            .with(csrf())
        )
        .andExpect(status().isForbidden());
    }

    @Test
    void nonExistentGameReturnsNotFound() throws Exception {
        User user = createUser("player@example.com", TEST_PASSWORD);

        mockMvc.perform(
            post("/game/999999/reveal")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"clientNonce\":\"" + "d".repeat(64) + "\"}")
            .with(user(new UserDetailsAdapter(user)))
            .with(csrf())
        )
        .andExpect(status().isNotFound());
    }

    private User createUser(String email, String password) {
        User user = new User("Test", "User", email, password);
        return userRepository.save(user);
    }

    private String sha256Hex(String input) throws Exception {
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = sha256.digest(input.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hashBytes);
    }
}

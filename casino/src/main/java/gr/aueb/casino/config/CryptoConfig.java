package gr.aueb.casino.config;

import java.security.SecureRandom;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CryptoConfig {

    @Bean
    public SecureRandom secureRandom() {
        return new SecureRandom();
    }
}

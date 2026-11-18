package com.example.userprojectjdbcsqlserver.config;

import org.springframework.boot.actuate.endpoint.SanitizableData;
import org.springframework.boot.actuate.endpoint.SanitizingFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.regex.Pattern;

/**
 * /actuator/configprops montre la configuration EFFECTIVE (apres surcharge par variables d'environnement, profils...).
 * Les valeurs sont affichees (management.endpoint.configprops.show-values: always) SAUF celles dont la cle ressemble a
 * un secret : elles restent masquees meme pour un administrateur. L'endpoint lui-meme exige PARAMETER_MANAGE (SecurityConfig).
 */
@Configuration
public class ActuatorConfig {

    private static final Pattern SECRET_KEY = Pattern.compile(".*(password|secret|key|token|credential).*");

    @Bean
    public SanitizingFunction secretsSanitizer() {
        return data -> {
            String key = data.getKey();
            return key != null && SECRET_KEY.matcher(key.toLowerCase()).matches() ? data.withValue("******") : data;
        };
    }
}

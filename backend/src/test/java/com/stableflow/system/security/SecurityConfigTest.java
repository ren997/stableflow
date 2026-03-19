package com.stableflow.system.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stableflow.system.config.SecurityProperties;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

class SecurityConfigTest {

    private final SecurityConfig securityConfig = new SecurityConfig();

    @Test
    void shouldCreateCorsConfigurationFromSecurityProperties() {
        SecurityProperties securityProperties = new SecurityProperties(
            "change-me-in-env",
            List.of("http://localhost:5173", "https://merchant.stableflow.com")
        );

        CorsConfigurationSource source = securityConfig.corsConfigurationSource(securityProperties);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/auth/login");
        CorsConfiguration configuration = source.getCorsConfiguration(request);

        assertEquals(List.of("http://localhost:5173", "https://merchant.stableflow.com"), configuration.getAllowedOrigins());
        assertTrue(configuration.getAllowedMethods().contains("OPTIONS"));
        assertEquals(List.of("*"), configuration.getAllowedHeaders());
        assertEquals(List.of("X-Trace-Id"), configuration.getExposedHeaders());
    }
}

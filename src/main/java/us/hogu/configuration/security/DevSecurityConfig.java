package us.hogu.configuration.security;

import us.hogu.configuration.security.filter.DevUserAccountValidationFilter;

import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import io.jsonwebtoken.security.Keys;

@Profile("dev")
@Configuration
@SuppressWarnings("deprecation")
public class DevSecurityConfig extends WebSecurityConfigurerAdapter {

    private final DevUserAccountValidationFilter userAccountValidationFilter;

    @Value("${jwt.secret:myDevJwtSecret_64_characters_minimum__________1234567890}")
    private String jwtSecret;

    public DevSecurityConfig(DevUserAccountValidationFilter userAccountValidationFilter) {
        this.userAccountValidationFilter = userAccountValidationFilter;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .cors() // ABILITA CORS
            .and()
            .authorizeRequests()
                .antMatchers(
                    "/", 
                    "/api/public/**",
                    "/api/v1/utente/registrazione-controllo",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/swagger",
                    "/webjars/**"
                ).permitAll()
                .anyRequest().authenticated()
            .and()
            .oauth2ResourceServer()
                .jwt()
                .decoder(primaryJwtDecoder())
                .jwtAuthenticationConverter(emptyAuthoritiesConverter());

        http.addFilterAfter(userAccountValidationFilter, BearerTokenAuthenticationFilter.class);
    }

    private JwtAuthenticationConverter emptyAuthoritiesConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> List.of());
        return converter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtDecoder primaryJwtDecoder() {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withSecretKey(key)
            .macAlgorithm(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS512)
            .build();

        jwtDecoder.setJwtValidator(new JwtTimestampValidator());

        return jwtDecoder;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of("https://localhost:5173", "https://192.168.0.104:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}

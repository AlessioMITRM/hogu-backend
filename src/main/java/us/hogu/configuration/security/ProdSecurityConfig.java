package us.hogu.configuration.security;

import us.hogu.configuration.security.filter.ProdUserAccountValidationFilter;
import us.hogu.configuration.security.filter.DevUserAccountValidationFilter;
import us.hogu.configuration.security.interfaces.JwtUserMapper;

import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Profile({"prod"})
@SuppressWarnings("deprecation")
@Configuration
public class ProdSecurityConfig extends WebSecurityConfigurerAdapter {

    private final ProdUserAccountValidationFilter userAccountMappingFilter;
    

    public ProdSecurityConfig(ProdUserAccountValidationFilter userAccountMappingFilter) {
    	this.userAccountMappingFilter = userAccountMappingFilter;
	}

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeRequests(requests -> requests
                // tutte le altre richieste richiedono autenticazione
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(server -> server
                .jwt()
                .decoder(primaryJwtDecoder())
                .jwtAuthenticationConverter(emptyAuthoritiesConverter())
            );

        // Filtro custom per il secondo JWT + ruolo
        http.addFilterAfter(userAccountMappingFilter, UsernamePasswordAuthenticationFilter.class);
    }

    /**
     * Converter che non assegna alcuna authority dal JWT principale.
     */
    private JwtAuthenticationConverter emptyAuthoritiesConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> java.util.List.of());
        return converter;
    }

    /**
     * Bean per il decoder del primo JWT (chiave pubblica RSA).
     */
    @Bean
    public org.springframework.security.oauth2.jwt.JwtDecoder primaryJwtDecoder() {
        String jwksUri = "https://sso-pre.agea.gov.it/auth/realms/agea_sso_realm/protocol/openid-connect/certs";
        return org.springframework.security.oauth2.jwt.NimbusJwtDecoder.withJwkSetUri(jwksUri).build();
    }
}

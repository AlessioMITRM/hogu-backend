package us.hogu.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

@Configuration
@Profile({"dev", "stag"})
public class RequestLoggingFilterConfig {

    @Bean
    public CommonsRequestLoggingFilter logFilter() {
        CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();
        filter.setIncludeQueryString(true); // Mostra i parametri ?...
        filter.setIncludePayload(true);   // Mostra il corpo JSON
        filter.setMaxPayloadLength(10000); // Aumenta la dimensione massima del corpo loggato
        filter.setIncludeHeaders(false);  // Opzionale, per non intasare i log
        filter.setAfterMessagePrefix("REQUEST DATA: ");
        return filter;
    }
}
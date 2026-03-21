package us.hogu.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;
import java.util.Set;

@Configuration
@RequiredArgsConstructor
public class DatabaseFixer {

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;
    private static final Logger logger = LoggerFactory.getLogger(DatabaseFixer.class);

    @Value("${hogu.redis.cleanupBnbAvailability:false}")
    private boolean cleanupBnbAvailability;

    @Bean
    public CommandLineRunner fixDatabaseConstraints() {
        return args -> {
            try {
                logger.info("Tentativo di correzione constraint database per bnb_bookings...");
                String sql = "ALTER TABLE hogu.bnb_bookings DROP CONSTRAINT IF EXISTS fkg51aapc44yd3y83y6e2jrc3rb";
                jdbcTemplate.execute(sql);
                logger.info("Constraint fkg51aapc44yd3y83y6e2jrc3rb rimosso con successo (se esisteva).");
                
                if (cleanupBnbAvailability) {
                    logger.info("Pulizia chiavi Redis per availability bnb attiva...");
                    Set<String> keys = redisTemplate.keys("bnb:room:*:availability:*");
                    if (keys != null && !keys.isEmpty()) {
                        redisTemplate.delete(keys);
                        logger.info("Cancellate " + keys.size() + " chiavi di disponibilità B&B da Redis.");
                    } else {
                        logger.info("Nessuna chiave B&B trovata in Redis.");
                    }
                }
                
            } catch (Exception e) {
                logger.error("Errore durante la correzione del database/redis: " + e.getMessage());
            }
        };
    }
}

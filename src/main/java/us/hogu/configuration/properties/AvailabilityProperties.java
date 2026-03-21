package us.hogu.configuration.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "availability")
public class AvailabilityProperties {
    private Warmup warmup = new Warmup();
    private RedisAsPrimary redisAsPrimary = new RedisAsPrimary();

    @Data
    public static class Warmup {
        private boolean enabled = false;
        private FlushNamespace flushNamespace = new FlushNamespace();

        @Data
        public static class FlushNamespace {
            private boolean enabled = false;
        }
    }

    @Data
    public static class RedisAsPrimary {
        private boolean enabled = false;
    }
}

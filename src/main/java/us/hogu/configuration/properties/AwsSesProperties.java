package us.hogu.configuration.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "aws.ses")
public class AwsSesProperties {
    private String senderEmail;
    private String region;
    private String accessKey;
    private String secretKey;
}

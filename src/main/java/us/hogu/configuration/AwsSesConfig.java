package us.hogu.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import us.hogu.configuration.properties.AwsSesProperties;

@Configuration
@RequiredArgsConstructor
public class AwsSesConfig {

    private final AwsSesProperties awsSesProperties;

    @Bean
    public SesClient sesClient() {
        return SesClient.builder()
                .region(Region.of(awsSesProperties.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(
                                awsSesProperties.getAccessKey(),
                                awsSesProperties.getSecretKey()
                        )
                ))
                .build();
    }
}


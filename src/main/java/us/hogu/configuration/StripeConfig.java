package us.hogu.configuration;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.stripe.Stripe;

@Configuration
public class StripeConfig {
    
    @Value("${stripe.secret-key}")
    private String stripeSecretKey;
    
    @Value("${stripe.publishable-key}")
    private String stripePublishableKey;
    
    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }
    
}
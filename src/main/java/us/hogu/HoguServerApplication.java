package us.hogu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EntityScan("us.hogu.model") 
@EnableFeignClients(basePackages = "us.hogu.client")
@EnableScheduling
public class HoguServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(HoguServerApplication.class, args);
    }
}
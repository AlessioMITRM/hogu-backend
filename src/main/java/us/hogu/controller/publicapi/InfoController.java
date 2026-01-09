package us.hogu.controller.publicapi;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.concurrent.ThreadLocalRandom;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/public/info")
public class InfoController {
    private volatile int restaurantValue = generateRandom();
    private volatile int bnbValue = generateRandom();
    private volatile int luggageValue = generateRandom();
    private volatile int eventValue = generateRandom();
    private volatile int clubValue = generateRandom();


    // Genera numero casuale tra 20 e 105
    private int generateRandom() {
        return ThreadLocalRandom.current().nextInt(20, 106);
    }

    // Ogni 15 minuti (00, 15, 30, 45)
    @Scheduled(cron = "0 0/15 * * * *")
    public void updateValue() {
    	restaurantValue = generateRandom();
    	bnbValue = generateRandom();
    	luggageValue = generateRandom();
    	eventValue = generateRandom();
    }

    @GetMapping("/restaurant")
    public int getRestaurantValue() {
        return restaurantValue;
    }
    
    @GetMapping("/bnb")
    public int getBnbValue() {
        return bnbValue;
    }

    @GetMapping("/luggage")
    public int getLuggageValue() {
        return luggageValue;
    }
    
    @GetMapping("/event")
    public int getEventValue() {
        return eventValue;
    }
    
    @GetMapping("/club")
    public int getClubValue() {
        return clubValue;
    }
    
}

package us.hogu.service.redis;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import us.hogu.model.User;
import us.hogu.model.enums.UserStatus;
import us.hogu.repository.jpa.UserJpa;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserStatusRedisService {

    private final StringRedisTemplate redisTemplate;
    private final UserJpa userJpa;

    private static final String USER_STATUS_KEY_PREFIX = "user:status:";
    private static final Duration STATUS_TTL = Duration.ofHours(24); // Ricarica la cache ogni 24h per sicurezza

    /**
     * Controlla se l'utente è attivo sfruttando Redis come cache.
     * Se la chiave non esiste, interroga il DB e la salva.
     */
    public boolean isUserActive(Long userId) {
        String key = USER_STATUS_KEY_PREFIX + userId;
        String statusCache = redisTemplate.opsForValue().get(key);

        if (statusCache != null) {
            return Boolean.parseBoolean(statusCache);
        }

        // Cache miss: carica dal DB
        try {
            User user = userJpa.findById(userId).orElse(null);
            boolean isActive = user != null && user.getStatus() == UserStatus.ACTIVE;

            // Per gli utenti provider, ammettiamo anche PENDING_ADMIN_APPROVAL per navigare
            // (solo login consentito/da verificare le rules del progetto)
            // Tipicamente si controlla che non sia BANNED, SUSPENDED o DEACTIVATED.
            if (user != null) {
                isActive = (user.getStatus() == UserStatus.ACTIVE
                        || user.getStatus() == UserStatus.PENDING_ADMIN_APPROVAL);
            } else {
                isActive = false;
            }

            redisTemplate.opsForValue().set(key, String.valueOf(isActive), STATUS_TTL);
            return isActive;
        } catch (Exception e) {
            log.error("Errore nel recupero stato utente dal DB (User: {})", userId, e);
            return false; // In caso di DB down, per sicurezza blocchiamo
        }
    }

    /**
     * Invalida la cache per l'utente, forzando la ri-lettura dal DB alla prossima
     * richiesta.
     */
    public void evictUserStatus(Long userId) {
        String key = USER_STATUS_KEY_PREFIX + userId;
        redisTemplate.delete(key);
    }
}

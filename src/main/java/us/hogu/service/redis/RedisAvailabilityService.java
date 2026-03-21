package us.hogu.service.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import us.hogu.common.constants.ErrorConstants;
import us.hogu.exception.ValidationException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RedisAvailabilityService {

    private final StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "bnb:room:";
    private static final String CLUB_EVENT_PREFIX = "club:event:";
    private static final String NCC_SERVICE_PREFIX = "ncc:service:";
    private static final String LUGGAGE_SERVICE_PREFIX = "luggage:service:";
    private static final String RESTAURANT_SERVICE_PREFIX = "restaurant:service:";
    private static final String BOOKING_RELEASED_PREFIX = "availability:booking:released:";
    private static final String BOOKING_WARMUP_APPLIED_PREFIX = "availability:warmup:applied:";

    /**
     * Lua script for atomic check-and-decrement with lazy initialization.
     * KEYS[1] = availability_key
     * ARGV[1] = requested_guests
     * ARGV[2] = max_capacity (used if key does not exist)
     * Returns: 1 if successful, 0 if not enough capacity
     */
    private static final String DECR_SCRIPT =
            "if redis.call('EXISTS', KEYS[1]) == 0 then " +
            "  redis.call('SET', KEYS[1], ARGV[2]) " +
            "end " +
            "local current = redis.call('GET', KEYS[1]) " +
            "if tonumber(current) >= tonumber(ARGV[1]) then " +
            "  redis.call('DECRBY', KEYS[1], ARGV[1]) " +
            "  return 1 " +
            "else " +
            "  return 0 " +
            "end";

    private static final String MULTI_DECR_SCRIPT = 
            "for i, key in ipairs(KEYS) do " +
            "  if redis.call('EXISTS', key) == 0 then " +
            "    redis.call('SET', key, ARGV[2]) " +
            "  end " +
            "  local current = redis.call('GET', key) " +
            "  if tonumber(current) < tonumber(ARGV[1]) then " +
            "    return 0 " +
            "  end " +
            "end " +
            "for i, key in ipairs(KEYS) do " +
            "  redis.call('DECRBY', key, ARGV[1]) " +
            "end " +
            "return 1";

    public boolean reserve(Long roomId, LocalDate date, int guests, int maxCapacity) {
        String key = getKey(roomId, date);
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(DECR_SCRIPT, Long.class);
        
        // args: guests, maxCapacity
        Long result = redisTemplate.execute(script, Collections.singletonList(key), 
                                            String.valueOf(guests), String.valueOf(maxCapacity));

        return result != null && result == 1;
    }

    public void rollback(Long roomId, LocalDate date, int guests) {
        String key = getKey(roomId, date);
        // Increment back only if key exists (it should if we reserved it)
        redisTemplate.opsForValue().increment(key, guests);
    }

    private String getKey(Long roomId, LocalDate date) {
        return KEY_PREFIX + roomId + ":availability:" + date;
    }

    private boolean executeScript(String key, int amount, int maxCapacity) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(DECR_SCRIPT, Long.class);
        Long result = redisTemplate.execute(script, Collections.singletonList(key),
                String.valueOf(amount), String.valueOf(maxCapacity));
        return result != null && result == 1;
    }

    public boolean reserveEvent(Long eventId, int people, int initialAvailableCapacity) {
        String key = getEventKey(eventId);
        return executeScript(key, people, initialAvailableCapacity);
    }

    public boolean checkEventAvailability(Long eventId, Long maxCapacity) {
        String key = getEventKey(eventId);
        String val = redisTemplate.opsForValue().get(key);
        
        // If val is null, it means no bookings yet, so availability = maxCapacity.
        // If maxCapacity > 0, then it is available.
        if (val == null) {
            // Se la chiave non esiste, dobbiamo crearla con la maxCapacity corrente
            // per evitare che rimanga null e futuri decrementi falliscano o partano da presupposti errati.
            // Tuttavia, per un semplice controllo (check), basta dire che se è null, è disponibile (se max > 0).
            // Ma attenzione: se l'evento è stato creato ma mai "toccato" su Redis, è disponibile.
            return maxCapacity != null && maxCapacity > 0;
        }
        return Integer.parseInt(val) > 0;
    }

    public boolean checkBnbAvailability(Long roomId, LocalDate checkIn, LocalDate checkOut, int maxCapacity) {
        if (checkIn == null || checkOut == null) return true;
        
        LocalDate current = checkIn;
        List<String> keys = new ArrayList<>();
        while (current.isBefore(checkOut)) {
            keys.add(KEY_PREFIX + roomId + ":availability:" + current.toString());
            current = current.plusDays(1);
        }

        if (keys.isEmpty()) return true;

        List<String> values = redisTemplate.opsForValue().multiGet(keys);
        if (values != null) {
            for (String val : values) {
                if (val != null) {
                    // If any day has 0 or less availability, return false
                    if (Integer.parseInt(val) <= 0) {
                        return false;
                    }
                }
            }
        }
        // If keys don't exist (null values), it implies maxCapacity availability (lazy init).
        // If maxCapacity <= 0, then it's unavailable.
        return maxCapacity > 0;
    }

    public boolean checkLuggageAvailability(Long serviceId, int maxCapacity) {
        OffsetDateTime now = OffsetDateTime.now().truncatedTo(ChronoUnit.HOURS);
        String key = LUGGAGE_SERVICE_PREFIX + serviceId + ":time:" + now.toEpochSecond();
        String val = redisTemplate.opsForValue().get(key);
        
        if (val == null) return maxCapacity > 0;
        return Integer.parseInt(val) > 0;
    }

    public boolean checkRestaurantAvailability(Long serviceId, int maxCapacity) {
        OffsetDateTime now = OffsetDateTime.now().truncatedTo(ChronoUnit.HOURS);
        String key = RESTAURANT_SERVICE_PREFIX + serviceId + ":time:" + now.toEpochSecond();
        String val = redisTemplate.opsForValue().get(key);
        
        if (val == null) return maxCapacity > 0;
        return Integer.parseInt(val) > 0;
    }

    public boolean checkNccAvailability(Long nccServiceId, OffsetDateTime pickupTime, int maxCapacity) {
        String key = getNccKey(nccServiceId, pickupTime);
        String val = redisTemplate.opsForValue().get(key);
        
        if (val == null) return maxCapacity > 0;
        return Integer.parseInt(val) > 0;
    }

    public boolean reserveNcc(Long nccServiceId, OffsetDateTime pickupTime, int maxCapacity) {
        String key = getNccKey(nccServiceId, pickupTime);
        // For NCC, we check if occupied < max. 
        // We use the same script logic: ARGV[1]=amount=1, ARGV[2]=maxCapacity
        // BUT: The script decrements availability. So we init with maxCapacity.
        // If current >= 1, we decrement. 
        // So this logic holds: "Do we have 1 vehicle available?"
        return executeScript(key, 1, maxCapacity);
    }

    public void rollbackLuggage(Long serviceId, OffsetDateTime dropOff, OffsetDateTime pickUp, int bags) {
        List<String> keys = getLuggageKeys(serviceId, dropOff, pickUp);
        for (String key : keys) {
            redisTemplate.opsForValue().increment(key, bags);
        }
    }

    public void rollbackEvent(Long eventId, int people) {
        String key = getEventKey(eventId);
        redisTemplate.opsForValue().increment(key, people);
    }

    public boolean reserveLuggage(Long serviceId, OffsetDateTime dropOff, OffsetDateTime pickUp, int bags, int maxCapacity) {
        List<String> keys = getLuggageKeys(serviceId, dropOff, pickUp);
        if (keys.isEmpty()) return true; // No overlap with hourly slots? Should not happen if duration > 0
        
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(MULTI_DECR_SCRIPT, Long.class);
        Long result = redisTemplate.execute(script, keys, String.valueOf(bags), String.valueOf(maxCapacity));
        return result != null && result == 1;
    }

    public void rollbackNcc(Long nccServiceId, OffsetDateTime pickupTime) {
        String key = getNccKey(nccServiceId, pickupTime);
        redisTemplate.opsForValue().increment(key, 1);
    }

    public void rollbackRestaurant(Long serviceId, OffsetDateTime reservationTime, int people) {
        OffsetDateTime endTime = reservationTime.plusHours(2);
        List<String> keys = getRestaurantKeys(serviceId, reservationTime, endTime);
        for (String key : keys) {
            redisTemplate.opsForValue().increment(key, people);
        }
    }
    
    public void rollbackBnb(Long roomId, LocalDate checkIn, LocalDate checkOut, int guests) {
        if (checkIn == null || checkOut == null) return;
        LocalDate current = checkIn;
        while (current.isBefore(checkOut)) {
            String key = getKey(roomId, current);
            redisTemplate.opsForValue().increment(key, guests);
            current = current.plusDays(1);
        }
    }

    public boolean reserveRestaurant(Long serviceId, OffsetDateTime reservationTime, int people, int maxCapacity) {
        // Assume 2 hours duration for restaurant booking
        OffsetDateTime endTime = reservationTime.plusHours(2);
        List<String> keys = getRestaurantKeys(serviceId, reservationTime, endTime);
        
        if (keys.isEmpty()) return true;

        DefaultRedisScript<Long> script = new DefaultRedisScript<>(MULTI_DECR_SCRIPT, Long.class);
        Long result = redisTemplate.execute(script, keys, String.valueOf(people), String.valueOf(maxCapacity));
        return result != null && result == 1;
    }

    public boolean checkRestaurant(Long serviceId, OffsetDateTime reservationTime, int people, int maxCapacity) {
        OffsetDateTime endTime = reservationTime.plusHours(2);
        List<String> keys = getRestaurantKeys(serviceId, reservationTime, endTime);
        
        if (keys.isEmpty()) return true;

        List<String> values = redisTemplate.opsForValue().multiGet(keys);
        if (values == null) return true;

        for (String val : values) {
            int available = val == null ? maxCapacity : Integer.parseInt(val);
            if (available < people) {
                return false;
            }
        }
        return true;
    }

    public void updateRestaurantCapacity(Long serviceId, int oldCapacity, int newCapacity) {
        if (oldCapacity == newCapacity) return;
        int diff = newCapacity - oldCapacity;
        
        String pattern = RESTAURANT_SERVICE_PREFIX + serviceId + ":time:*";
        
        List<String> keys = new ArrayList<>();
        redisTemplate.execute((RedisConnection connection) -> {
            try (Cursor<byte[]> cursor = connection.scan(ScanOptions.scanOptions().match(pattern).count(100).build())) {
                while (cursor.hasNext()) {
                    keys.add(new String(cursor.next()));
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return null;
        });

        // Phase 1: Check (only if reducing capacity)
        if (diff < 0) {
            if (!keys.isEmpty()) {
                List<String> values = redisTemplate.opsForValue().multiGet(keys);
                if (values != null) {
                    for (String val : values) {
                        if (val != null) {
                            int currentAvail = Integer.parseInt(val);
                            if (currentAvail + diff < 0) {
                                throw new ValidationException(
                                    "CAPACITY_ERROR", 
                                    "Impossibile ridurre la capacità: ci sono prenotazioni esistenti che superano il nuovo limite."
                                );
                            }
                        }
                    }
                }
            }
        }
        
        // Phase 2: Update
        for (String key : keys) {
            redisTemplate.opsForValue().increment(key, diff);
        }
    }

    public void updateNccCapacity(Long serviceId, int oldCapacity, int newCapacity) {
        if (oldCapacity == newCapacity) return;
        int diff = newCapacity - oldCapacity;
        
        String pattern = NCC_SERVICE_PREFIX + serviceId + ":time:*";
        
        List<String> keys = new ArrayList<>();
        redisTemplate.execute((RedisConnection connection) -> {
            try (Cursor<byte[]> cursor = connection.scan(ScanOptions.scanOptions().match(pattern).count(100).build())) {
                while (cursor.hasNext()) {
                    keys.add(new String(cursor.next()));
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return null;
        });

        // Phase 1: Check (only if reducing capacity)
        if (diff < 0) {
            if (!keys.isEmpty()) {
                List<String> values = redisTemplate.opsForValue().multiGet(keys);
                if (values != null) {
                    for (String val : values) {
                        if (val != null) {
                            int currentAvail = Integer.parseInt(val);
                            if (currentAvail + diff < 0) {
                                throw new ValidationException(
                                    "CAPACITY_ERROR", 
                                    "Impossibile ridurre la capacità (NCC): ci sono prenotazioni esistenti che superano il nuovo limite."
                                );
                            }
                        }
                    }
                }
            }
        }
        
        // Phase 2: Update
        for (String key : keys) {
            redisTemplate.opsForValue().increment(key, diff);
        }
    }

    public void updateLuggageCapacity(Long serviceId, int oldCapacity, int newCapacity) {
        if (oldCapacity == newCapacity) return;
        int diff = newCapacity - oldCapacity;

        String pattern = LUGGAGE_SERVICE_PREFIX + serviceId + ":time:*";

        List<String> keys = new ArrayList<>();
        redisTemplate.execute((RedisConnection connection) -> {
            try (Cursor<byte[]> cursor = connection.scan(ScanOptions.scanOptions().match(pattern).count(100).build())) {
                while (cursor.hasNext()) {
                    keys.add(new String(cursor.next()));
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return null;
        });

        // Phase 1: Check (only if reducing capacity)
        if (diff < 0) {
            if (!keys.isEmpty()) {
                List<String> values = redisTemplate.opsForValue().multiGet(keys);
                if (values != null) {
                    for (String val : values) {
                        if (val != null) {
                            int currentAvail = Integer.parseInt(val);
                            if (currentAvail + diff < 0) {
                                throw new ValidationException(
                                        "CAPACITY_ERROR",
                                        "Impossibile ridurre la capacità (Luggage): ci sono prenotazioni esistenti che superano il nuovo limite."
                                );
                            }
                        }
                    }
                }
            }
        }

        // Phase 2: Update
        for (String key : keys) {
            redisTemplate.opsForValue().increment(key, diff);
        }
    }

    public void updateEventCapacity(Long eventId, Long oldCapacity, Long newCapacity) {
        if (oldCapacity == newCapacity) return;
        Long diff = newCapacity - oldCapacity;
        
        // Key for event is just one key: club:event:{eventId}
        String key = getEventKey(eventId);
        
        // Check if key exists
        Boolean exists = redisTemplate.hasKey(key);
        if (Boolean.FALSE.equals(exists)) {
            return;
        }

        // Phase 1: Check (only if reducing capacity)
        if (diff < 0) {
            String val = redisTemplate.opsForValue().get(key);
            if (val != null) {
                int currentAvail = Integer.parseInt(val);
           
                if (currentAvail + diff < 0) {
                    throw new ValidationException(
                            ErrorConstants.CAPACITY_ERROR.name(),
                            ErrorConstants.CAPACITY_ERROR.getMessage()
                    );
                }
            }
        }

        redisTemplate.opsForValue().increment(key, diff);
    }

    public void updateBnbRoomCapacity(Long roomId, int oldCapacity, int newCapacity) {
        if (oldCapacity == newCapacity) return;
        int diff = newCapacity - oldCapacity;

        String pattern = KEY_PREFIX + roomId + ":availability:*";

        List<String> keys = new ArrayList<>();
        redisTemplate.execute((RedisConnection connection) -> {
            try (Cursor<byte[]> cursor = connection.scan(ScanOptions.scanOptions().match(pattern).count(100).build())) {
                while (cursor.hasNext()) {
                    keys.add(new String(cursor.next()));
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return null;
        });

        // Phase 1: Check (only if reducing capacity)
        if (diff < 0) {
            if (!keys.isEmpty()) {
                List<String> values = redisTemplate.opsForValue().multiGet(keys);
                if (values != null) {
                    for (String val : values) {
                        if (val != null) {
                            int currentAvail = Integer.parseInt(val);
                            if (currentAvail + diff < 0) {
                                throw new ValidationException(
                                        "CAPACITY_ERROR",
                                        "Impossibile ridurre la capacità (B&B Room): ci sono prenotazioni esistenti che superano il nuovo limite."
                                );
                            }
                        }
                    }
                }
            }
        }

        // Phase 2: Update
        for (String key : keys) {
            redisTemplate.opsForValue().increment(key, diff);
        }
    }

    public boolean markBookingReleasedOnce(Long bookingId) {
        if (bookingId == null) return false;
        String key = BOOKING_RELEASED_PREFIX + bookingId;
        Boolean ok = redisTemplate.opsForValue().setIfAbsent(key, "1");
        return Boolean.TRUE.equals(ok);
    }

    public boolean markWarmupAppliedOnce(Long bookingId) {
        if (bookingId == null) return false;
        String key = BOOKING_WARMUP_APPLIED_PREFIX + bookingId;
        Boolean ok = redisTemplate.opsForValue().setIfAbsent(key, "1");
        return Boolean.TRUE.equals(ok);
    }

    public long deleteByPattern(String pattern) {
        if (pattern == null || pattern.isBlank()) return 0;

        List<String> keys = new ArrayList<>();
        redisTemplate.execute((RedisConnection connection) -> {
            try (Cursor<byte[]> cursor = connection.scan(ScanOptions.scanOptions().match(pattern).count(500).build())) {
                while (cursor.hasNext()) {
                    keys.add(new String(cursor.next()));
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return null;
        });

        if (keys.isEmpty()) return 0;
        Long deleted = redisTemplate.delete(keys);
        return deleted == null ? 0 : deleted;
    }


    private List<String> getLuggageKeys(Long serviceId, OffsetDateTime dropOff, OffsetDateTime pickUp) {
        List<String> keys = new ArrayList<>();
        // Truncate to hour to get the "slot"
        OffsetDateTime current = dropOff.truncatedTo(ChronoUnit.HOURS);
        // We consider the slot occupied if the reservation touches it.
        // E.g. 10:50 to 11:10.
        // 10:00-11:00 is occupied. 11:00-12:00 is occupied.
        // Current logic:
        // current = 10:00. 
        // while current < pickUp:
        //   add key
        //   current += 1 hour
        // If pickUp is 11:10, loop:
        // 1. 10:00 < 11:10 -> add 10:00. next: 11:00
        // 2. 11:00 < 11:10 -> add 11:00. next: 12:00
        // 3. 12:00 >= 11:10 -> stop.
        // Correct.
        
        while (current.isBefore(pickUp)) {
            keys.add(LUGGAGE_SERVICE_PREFIX + serviceId + ":time:" + current.toEpochSecond());
            current = current.plusHours(1);
        }
        return keys;
    }

    private String getEventKey(Long eventId) {
        return CLUB_EVENT_PREFIX + eventId;
    }

    private String getNccKey(Long nccServiceId, OffsetDateTime pickupTime) {
        // Normalize time to minutes or use epoch seconds to match exact request time
        // Since the current logic checks exact equality, we use epoch seconds
        return NCC_SERVICE_PREFIX + nccServiceId + ":time:" + pickupTime.toEpochSecond();
    }

    private List<String> getRestaurantKeys(Long serviceId, OffsetDateTime start, OffsetDateTime end) {
        List<String> keys = new ArrayList<>();
        OffsetDateTime current = start.truncatedTo(ChronoUnit.HOURS);
        while (current.isBefore(end)) {
            keys.add(RESTAURANT_SERVICE_PREFIX + serviceId + ":time:" + current.toEpochSecond());
            current = current.plusHours(1);
        }
        return keys;
    }
}

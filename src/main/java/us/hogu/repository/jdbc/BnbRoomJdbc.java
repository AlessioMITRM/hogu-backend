package us.hogu.repository.jdbc;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import us.hogu.controller.dto.request.BnbSearchRequestDto;
import us.hogu.controller.dto.response.BnbRoomResponseDto;
import us.hogu.controller.dto.response.BnbSearchResponseDto.BnbSearchResultDto;
import us.hogu.exception.ResourceNotFoundException;
import us.hogu.controller.dto.response.ServiceLocaleResponseDto;
import us.hogu.model.BnbRoom;
import us.hogu.model.enums.ServiceType;
import us.hogu.repository.jpa.BnbRoomJpa;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Repository
public class BnbRoomJdbc {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final BnbRoomJpa bnbRoomJpa;

    public Page<BnbSearchResultDto> searchNative(BnbSearchRequestDto request, Pageable pageable, String language) {

        String cityParam = null;
        String stateParam = null;

        if (request.getLocation() != null && !request.getLocation().trim().isEmpty()) {
            String[] parts = request.getLocation().split(",");
            cityParam = parts[0].trim();
            if (parts.length > 1) stateParam = parts[1].trim();
        }

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("searchTerm", request.getSearchTerm() != null ? "%" + request.getSearchTerm() + "%" : null);
        params.addValue("city", cityParam != null ? "%" + cityParam + "%" : null);
        params.addValue("state", stateParam != null ? "%" + stateParam + "%" : null);
        params.addValue("checkIn", request.getCheckIn());
        params.addValue("checkOut", request.getCheckOut());
        params.addValue("totalGuests", request.getAdults() + request.getChildren());
        params.addValue("limit", pageable.getPageSize());
        params.addValue("offset", pageable.getOffset());
        params.addValue("language", language);

        StringBuilder sql = new StringBuilder();
        sql.append(" FROM hogu.bnb_room r ");
        sql.append(" JOIN hogu.bnb_services bs ON r.bnb_service_id = bs.id ");
        sql.append(" LEFT JOIN hogu.service_locales sl ON bs.id = sl.bnb_service_id AND sl.language = :language ");
        sql.append(" WHERE 1=1 ");
        sql.append(" AND r.max_guests >= :totalGuests ");

        if (request.getSearchTerm() != null) {
            sql.append(" AND (LOWER(CAST(r.name AS TEXT)) LIKE LOWER(:searchTerm) ")
               .append(" OR LOWER(CAST(sl.address AS TEXT)) LIKE LOWER(:searchTerm)) ");
        }

        if (cityParam != null) {
            sql.append(" AND LOWER(CAST(sl.city AS TEXT)) LIKE LOWER(:city) ");
        }

        if (stateParam != null) {
            sql.append(" AND LOWER(CAST(sl.state AS TEXT)) LIKE LOWER(:state) ");
        }

        // Controllo disponibilità tramite BnbRoomAvailability
        if (request.getCheckIn() != null && request.getCheckOut() != null) {
            sql.append(" AND NOT EXISTS ( ");
            sql.append("     SELECT 1 FROM hogu.bnb_room_availability a ");
            sql.append("     WHERE a.room_id = r.id ");
            sql.append("       AND a.date >= :checkIn ");
            sql.append("       AND a.date < :checkOut ");
            sql.append("       AND a.occupied_capacity >= a.capacity ");
            sql.append(" ) ");
        }

        String countSql = "SELECT COUNT(DISTINCT r.id) " + sql;
        Integer total = jdbcTemplate.queryForObject(countSql, params, Integer.class);

        String selectSql =
                "SELECT DISTINCT r.id, r.name, r.description, r.base_price_per_night, r.max_guests, " +
                "bs.images, sl.country, sl.state, sl.city, sl.address " +
                sql +
                " ORDER BY r.base_price_per_night ASC " +
                " LIMIT :limit OFFSET :offset";

        List<BnbSearchResultDto> results = jdbcTemplate.query(
                selectSql,
                params,
                (rs, rowNum) -> mapRoomToDto(rs, request.getCheckIn(), request.getCheckOut(), language)
        );

        return new PageImpl<>(results, pageable, total != null ? total : 0);
    }

    public BnbRoomResponseDto getRoomById(Long id, LocalDate checkIn, LocalDate checkOut, String language) {

        BnbRoom entity = bnbRoomJpa.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Servizio B&B non trovato"));

        Long roomId = entity.getId();
        Long serviceId = entity.getBnbService().getId();

        Double basePrice = entity.getBasePricePerNight();
        double totalPrice = calculateTotalPrice(roomId, basePrice, checkIn, checkOut);

        long nights = (checkIn != null && checkOut != null)
                ? ChronoUnit.DAYS.between(checkIn, checkOut)
                : 0;
        double pricePerNight = nights > 0 ? totalPrice / nights : basePrice;

        // Controllo disponibilità tramite NamedParameterJdbcTemplate
        boolean available = true;
        if (checkIn != null && checkOut != null) {
            String sqlAvailability =
                "SELECT occupied_capacity FROM bnb_room_availability " +
                "WHERE room_id = :roomId AND date >= :checkIn AND date < :checkOut";

            MapSqlParameterSource availabilityParams = new MapSqlParameterSource();
            availabilityParams.addValue("roomId", roomId);
            availabilityParams.addValue("checkIn", checkIn);
            availabilityParams.addValue("checkOut", checkOut);

            List<Long> bookedCounts = jdbcTemplate.queryForList(
                sqlAvailability,
                availabilityParams,
                Long.class
            );

            for (Long count : bookedCounts) {
                if (count >= entity.getMaxGuests()) {
                    available = false;
                    break;
                }
            }
        }

        List<String> images = entity.getImages();
        List<ServiceLocaleResponseDto> locales = loadLocalesForLanguage(roomId, language);

        return BnbRoomResponseDto.builder()
                .id(roomId)
                .bnbServiceId(serviceId)
                .name(entity.getName())
                .providerName(entity.getBnbService().getName())
                .description(entity.getDescription())
                .maxGuests(entity.getMaxGuests())
                .totalPrice(totalPrice)
                .priceForNight(pricePerNight)
                .available(available)
                .images(images)
                .serviceLocale(locales)
                .build();
    }

    private BnbSearchResultDto mapRoomToDto(ResultSet rs, LocalDate checkIn, LocalDate checkOut, String language)
            throws SQLException {

        Long roomId = rs.getLong("id");
        Double basePrice = rs.getDouble("base_price_per_night");

        double totalPrice = calculateTotalPrice(roomId, basePrice, checkIn, checkOut);

        long nights = (checkIn != null && checkOut != null)
                ? ChronoUnit.DAYS.between(checkIn, checkOut)
                : 0;

        double pricePerNight = nights > 0 ? totalPrice / nights : basePrice;

        return BnbSearchResultDto.builder()
                .id(String.valueOf(roomId))
                .name(rs.getString("name"))
                .description(rs.getString("description"))
                .price(totalPrice)
                .pricePerNight(pricePerNight)
                .maxGuests(rs.getInt("max_guests"))
                .images(parseImages(rs.getString("images")))
                .locales(loadLocalesForLanguage(roomId, language))
                .build();
    }

    private double calculateTotalPrice(Long roomId, Double basePrice, LocalDate checkIn, LocalDate checkOut) {

        if (checkIn == null || checkOut == null) {
            return basePrice;
        }

        long days = ChronoUnit.DAYS.between(checkIn, checkOut);

        String sql =
                "SELECT start_date, end_date, price_per_night " +
                "FROM hogu.bnb_room_price_calendar " +
                "WHERE room_id = :roomId " +
                "  AND end_date >= :checkIn " +
                "  AND start_date <= :checkOut " +
                "ORDER BY start_date ";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("roomId", roomId)
                .addValue("checkIn", checkIn)
                .addValue("checkOut", checkOut);

        List<PriceRange> ranges = jdbcTemplate.query(
                sql,
                params,
                (rs, row) -> new PriceRange(
                        rs.getDate("start_date").toLocalDate(),
                        rs.getDate("end_date").toLocalDate(),
                        rs.getDouble("price_per_night"))
        );

        double total = 0;

        for (long i = 0; i < days; i++) {
            LocalDate day = checkIn.plusDays(i);

            Double price = ranges.stream()
                    .filter(r -> !day.isBefore(r.getStart()) && !day.isAfter(r.getEnd()))
                    .map(PriceRange::getPrice)
                    .findFirst()
                    .orElse(basePrice);

            total += price;
        }

        return total;
    }

    private static class PriceRange {
        private final LocalDate start;
        private final LocalDate end;
        private final double price;

        public PriceRange(LocalDate start, LocalDate end, double price) {
            this.start = start;
            this.end = end;
            this.price = price;
        }

        public LocalDate getStart() { return start; }
        public LocalDate getEnd() { return end; }
        public double getPrice() { return price; }
    }

    private List<ServiceLocaleResponseDto> loadLocalesForLanguage(Long roomId, String language) {

        String sql =
                "SELECT sl.id, bs.id AS service_id, sl.service_type, sl.language, " +
                "       sl.country, sl.state, sl.city, sl.address " +
                "FROM hogu.service_locales sl " +
                "JOIN hogu.bnb_services bs ON sl.bnb_service_id = bs.id " +
                "JOIN hogu.bnb_room r ON r.bnb_service_id = bs.id " +
                "WHERE r.id = :roomId " +
                "  AND sl.language = :language";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("roomId", roomId)
                .addValue("language", language);

        return jdbcTemplate.query(
                sql,
                params,
                (rs, rowNum) -> ServiceLocaleResponseDto.builder()
                        .serviceId(rs.getLong("service_id"))
                        .serviceType(ServiceType.values()[rs.getInt("service_type")])
                        .language(rs.getString("language"))
                        .country(rs.getString("country"))
                        .state(rs.getString("state"))
                        .city(rs.getString("city"))
                        .address(rs.getString("address"))
                        .build()
        );
    }

    private List<String> parseImages(String raw) {
        if (raw == null) return List.of();
        raw = raw.replace("{", "").replace("}", "");
        if (raw.trim().isEmpty()) return List.of();
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .collect(Collectors.toList());
    }
}

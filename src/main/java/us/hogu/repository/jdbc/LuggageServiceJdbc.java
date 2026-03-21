package us.hogu.repository.jdbc;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import us.hogu.controller.dto.request.LuggageSearchRequestDto;
import us.hogu.controller.dto.response.LuggageSearchResultResponseDto;
import us.hogu.controller.dto.response.LuggageSizePriceResponseDto;

@Repository
@RequiredArgsConstructor
public class LuggageServiceJdbc {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public Page<LuggageSearchResultResponseDto> searchNative(LuggageSearchRequestDto request, Pageable pageable) {

        // 1. Calcolo totale bagagli richiesti
        int requestedBags =
                ((request.getBagsS() != null ? request.getBagsS() : 0) +
                 (request.getBagsM() != null ? request.getBagsM() : 0) +
                 (request.getBagsL() != null ? request.getBagsL() : 0));
        if (requestedBags == 0) requestedBags = 1;

        // 2. Parametri SQL
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("dropOff", request.getDropOff() != null ? request.getDropOff().toLocalDate() : null);
        params.addValue("pickUp", request.getPickUp() != null ? request.getPickUp().toLocalDate() : null);
        params.addValue("requestedBags", requestedBags);
        params.addValue("limit", pageable.getPageSize());
        params.addValue("offset", pageable.getOffset());

        String countryParam = null;
        String provinceParam = null;
        
        if (request.getLocale() != null) {
             if (request.getLocale().getProvince() != null && !request.getLocale().getProvince().trim().isEmpty()) {
                 provinceParam = request.getLocale().getProvince().trim();
             }
             if (request.getLocale().getCountry() != null && !request.getLocale().getCountry().trim().isEmpty()) {
                 countryParam = request.getLocale().getCountry().trim();
             }
        }

        params.addValue("province", provinceParam != null ? "%" + provinceParam + "%" : null);
        params.addValue("country", countryParam != null ? "%" + countryParam + "%" : null);

        // 3. Costruzione query base
        StringBuilder sql = new StringBuilder();
        sql.append(" FROM hogu.luggage_services ls ");
        sql.append(" LEFT JOIN hogu.service_locales sl ON ls.id = sl.luggage_service_id ");
        sql.append(" WHERE 1=1 ");

        if (countryParam != null) {
            sql.append(" AND LOWER(CAST(sl.country AS TEXT)) LIKE LOWER(:country) ");
        }
        if (provinceParam != null) {
            sql.append(" AND LOWER(CAST(sl.province AS TEXT)) LIKE LOWER(:province) ");
        }

        // 4. Count totale
        String countSql = "SELECT COUNT(DISTINCT ls.id) " + sql.toString();
        Integer total = jdbcTemplate.queryForObject(countSql, params, Integer.class);

        // 5. Select dati
        String selectSql = "SELECT DISTINCT ls.id, ls.name, ls.description, sl.address, sl.city, ls.base_price, ls.images " +
                           sql.toString() +
                           " ORDER BY ls.base_price ASC " +
                           " LIMIT :limit OFFSET :offset";

        List<LuggageSearchResultResponseDto> results =
                jdbcTemplate.query(selectSql, params, (rs, rowNum) -> mapRowToDto(rs));

        if (!results.isEmpty()) {
            List<Long> serviceIds = results.stream()
                    .map(LuggageSearchResultResponseDto::getId)
                    .collect(Collectors.toList());

            String priceSql = "SELECT luggage_service_id, size_label, price_per_hour, price_per_day, description " +
                              "FROM hogu.luggage_size_prices " +
                              "WHERE luggage_service_id IN (:ids)";

            MapSqlParameterSource priceParams = new MapSqlParameterSource("ids", serviceIds);

            List<Map<String, Object>> pricesData = jdbcTemplate.queryForList(priceSql, priceParams);

            Map<Long, List<LuggageSizePriceResponseDto>> pricesMap = pricesData.stream()
                    .collect(Collectors.groupingBy(
                            row -> ((Number) row.get("luggage_service_id")).longValue(),
                            Collectors.mapping(row -> {
                                LuggageSizePriceResponseDto dto = new LuggageSizePriceResponseDto();
                                dto.setSizeLabel((String) row.get("size_label"));
                                dto.setPricePerHour(toBigDecimal(row.get("price_per_hour")));
                                dto.setPricePerDay(toBigDecimal(row.get("price_per_day")));
                                dto.setDescription((String) row.get("description"));
                                return dto;
                            }, Collectors.toList())
                    ));

            results.forEach(res -> {
                res.setSizePrices(pricesMap.getOrDefault(res.getId(), new ArrayList<>()));
            });
        }

        return new PageImpl<>(results, pageable, total != null ? total : 0);
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    // Mappa ResultSet nel DTO
    private LuggageSearchResultResponseDto mapRowToDto(ResultSet rs) throws SQLException {
        String rawImages = rs.getString("images");
        String firstImage = null;
        if (rawImages != null && !rawImages.isEmpty()) {
            String clean = rawImages.replace("[", "").replace("]", "").replace("\"", "");
            firstImage = clean.contains(",") ? clean.split(",")[0].trim() : clean.trim();
        }

        return LuggageSearchResultResponseDto.builder()
                .id(rs.getLong("id"))
                .name(rs.getString("name"))
                .description(rs.getString("description"))
                .address(rs.getString("address"))
                .locale(rs.getString("city"))
                .price(rs.getBigDecimal("base_price"))
                .imageUrl(firstImage)
                .build();
    }

    // Controllo disponibilità per range di date
    public boolean isAvailable(Long serviceId, LocalDate dropOff, LocalDate pickUp, long requestedBags) {
        String sql =
            "SELECT COUNT(*) " +
            "FROM hogu.luggage_service_availability a " +
            "WHERE a.luggage_service_id = :serviceId " +
            "  AND a.date >= :dropOff " +
            "  AND a.date < :pickUp " +
            "  AND a.occupied_capacity + :requestedBags > a.max_capacity";

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("serviceId", serviceId)
            .addValue("dropOff", dropOff)
            .addValue("pickUp", pickUp)
            .addValue("requestedBags", requestedBags);

        Integer count = jdbcTemplate.queryForObject(sql, params, Integer.class);
        return count != null && count == 0;
    }
}

package us.hogu.repository.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import us.hogu.controller.dto.request.LuggageSearchRequestDto;
import us.hogu.controller.dto.response.LuggageSearchResultResponseDto;

@Repository
@RequiredArgsConstructor
public class LuggageServiceJdbc {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public Page<LuggageSearchResultResponseDto> searchNative(LuggageSearchRequestDto request, Pageable pageable) {

        // 1. Gestione location
        String cityParam = null;
        String stateParam = null;
        if (request.getLocation() != null && !request.getLocation().trim().isEmpty()) {
            String[] parts = request.getLocation().split(",");
            cityParam = parts[0].trim();
            if (parts.length > 1) stateParam = parts[1].trim();
        }

        // 2. Calcolo totale bagagli richiesti
        int requestedBags =
                ((request.getBagsS() != null ? request.getBagsS() : 0) +
                 (request.getBagsM() != null ? request.getBagsM() : 0) +
                 (request.getBagsL() != null ? request.getBagsL() : 0));
        if (requestedBags == 0) requestedBags = 1;

        // 3. Parametri SQL
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("city", cityParam != null ? "%" + cityParam + "%" : null);
        params.addValue("state", stateParam != null ? "%" + stateParam + "%" : null);
        params.addValue("dropOff", request.getDropOff() != null ? request.getDropOff().toLocalDate() : null);
        params.addValue("pickUp", request.getPickUp() != null ? request.getPickUp().toLocalDate() : null);
        params.addValue("requestedBags", requestedBags);
        params.addValue("limit", pageable.getPageSize());
        params.addValue("offset", pageable.getOffset());

        // 4. Costruzione query base
        StringBuilder sql = new StringBuilder();
        sql.append(" FROM luggage_services ls ");
        sql.append(" JOIN service_locales sl ON ls.id = sl.luggage_service_id ");
        sql.append(" WHERE ls.publication_status = true ");

        // Filtri geografici
        if (cityParam != null) sql.append(" AND LOWER(CAST(sl.city AS TEXT)) LIKE LOWER(:city) ");
        if (stateParam != null) sql.append(" AND LOWER(CAST(sl.state AS TEXT)) LIKE LOWER(:state) ");

        // Controllo capacità statica
        sql.append(" AND COALESCE(ls.capacity, 0) >= :requestedBags ");

        // Controllo disponibilità dinamica con LuggageServiceAvailability
        if (request.getDropOff() != null && request.getPickUp() != null) {
            sql.append(" AND NOT EXISTS ( ");
            sql.append("   SELECT 1 FROM luggage_service_availability a ");
            sql.append("   WHERE a.luggage_service_id = ls.id ");
            sql.append("     AND a.date >= :dropOff ");
            sql.append("     AND a.date < :pickUp ");
            sql.append("     AND a.occupied_capacity + :requestedBags > a.max_capacity ");
            sql.append(" ) ");
        }

        // 5. Count totale
        String countSql = "SELECT COUNT(DISTINCT ls.id) " + sql.toString();
        Integer total = jdbcTemplate.queryForObject(countSql, params, Integer.class);

        // 6. Select dati
        String selectSql = "SELECT DISTINCT ls.id, ls.name, ls.description, sl.address, sl.city, ls.base_price, ls.images " +
                           sql.toString() +
                           " ORDER BY ls.base_price ASC " +
                           " LIMIT :limit OFFSET :offset";

        List<LuggageSearchResultResponseDto> results =
                jdbcTemplate.query(selectSql, params, (rs, rowNum) -> mapRowToDto(rs));

        return new PageImpl<>(results, pageable, total != null ? total : 0);
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
                .location(rs.getString("city"))
                .price(rs.getDouble("base_price"))
                .imageUrl(firstImage)
                .build();
    }

    // Controllo disponibilità per range di date
    public boolean isAvailable(Long serviceId, LocalDate dropOff, LocalDate pickUp, long requestedBags) {
        String sql =
            "SELECT COUNT(*) " +
            "FROM luggage_service_availability a " +
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

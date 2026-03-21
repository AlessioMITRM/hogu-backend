package us.hogu.repository.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import us.hogu.controller.dto.response.EventPublicResponseDto;

import java.util.stream.Collectors;
import java.util.Collections;
import java.util.Map;
import us.hogu.controller.dto.response.EventPricingConfigurationResponseDto;
import us.hogu.model.enums.PricingType;

@Repository
@RequiredArgsConstructor
public class ClubEventJdbc {
	private final NamedParameterJdbcTemplate jdbcTemplate;

	
	public Page<EventPublicResponseDto> getEventsForPublicWithFilters(
	        String location, // Ignored now
	        String provinceParam,
	        String countryParam,
	        String eventType,
	        String date,
	        Boolean table,
	        String language,
	        Pageable pageable
	) {
	    // Ignore location string parsing
	    // String cityParam = null; ...

	    OffsetDateTime startDate = null;
	    OffsetDateTime endDate = null;

	    if (date != null && !date.isEmpty()) {
	        try {
	            if (date.length() == 10) {
	                startDate = LocalDate.parse(date).atStartOfDay().atOffset(ZoneOffset.UTC);
	                endDate = startDate.plusDays(1);
	            } else {
	                startDate = OffsetDateTime.parse(date);
	                endDate = startDate.plusDays(1);
	            }
	        } catch (Exception e) {
	            throw new IllegalArgumentException("Invalid date format: " + date);
	        }
	    }

	    MapSqlParameterSource params = new MapSqlParameterSource();
	    params.addValue("eventType", eventType != null ? "%" + eventType + "%" : null);
	    params.addValue("country", countryParam != null ? "%" + countryParam.trim() + "%" : null);
	    params.addValue("province", provinceParam != null ? "%" + provinceParam.trim() + "%" : null);
	    params.addValue("startDate", startDate);
	    params.addValue("endDate", endDate);
	    params.addValue("limit", pageable.getPageSize());
	    params.addValue("offset", pageable.getOffset());

	    StringBuilder sql = new StringBuilder();

	    sql.append(" FROM hogu.event_club_services e ");
	    sql.append(" JOIN hogu.club_services cs ON e.club_service_id = cs.id ");
	    sql.append(" LEFT JOIN hogu.service_locales sl ON sl.event_id = e.id ");
	    sql.append(" WHERE 1=1 ");
	    sql.append(" AND e.is_active = true ");
	    sql.append(" AND cs.publication_status = true ");

	    if (startDate != null) {
	        sql.append(" AND e.start_time >= :startDate ");
	        sql.append(" AND e.start_time < :endDate ");
	    } else {
	        sql.append(" AND e.start_time >= NOW() ");
	    }

	    if (countryParam != null) {
	        sql.append(" AND LOWER(CAST(sl.country AS TEXT)) LIKE LOWER(:country) ");
	    }

	    if (provinceParam != null) {
	        sql.append(" AND LOWER(CAST(sl.province AS TEXT)) LIKE LOWER(:province) ");
	    }

	    if (eventType != null) {
	        sql.append(" AND (LOWER(e.name) LIKE LOWER(:eventType) ");
	        sql.append(" OR LOWER(e.theme) LIKE LOWER(:eventType) ");
	        sql.append(" OR LOWER(e.dj_name) LIKE LOWER(:eventType)) ");
	    }

	    sql.append(" AND (e.max_capacity > COALESCE(e.occupied_capacity, 0)) ");

	    if (Boolean.TRUE.equals(table)) {
	        sql.append(" AND EXISTS ( ");
	        sql.append("    SELECT 1 FROM hogu.event_pricing_configurations ep ");
	        sql.append("    WHERE ep.event_club_service_id = e.id ");
	        sql.append("    AND ep.pricing_type = 'TABLE' ");
	        sql.append(" ) ");
	    }

	    String countSql = "SELECT COUNT(DISTINCT e.id) " + sql;
	    Integer total = jdbcTemplate.queryForObject(countSql, params, Integer.class);

	    String selectSql =
	            "SELECT DISTINCT e.id, e.club_service_id, e.name, e.description, e.start_time, e.end_time, " +
	            "e.price, e.dj_name, e.theme, e.images, e.max_capacity, sl.city, sl.address, cs.name AS club_name, " +

	            " (SELECT MIN(ep.price) FROM hogu.event_pricing_configurations ep " +
	            "   WHERE ep.event_club_service_id = e.id AND ep.pricing_type = 'MAN') AS price_man, " +

	            " (SELECT MIN(ep.price) FROM hogu.event_pricing_configurations ep " +
	            "   WHERE ep.event_club_service_id = e.id AND ep.pricing_type = 'WOMAN') AS price_woman, " +

	            " (SELECT MIN(ep.price) FROM hogu.event_pricing_configurations ep " +
	            "   WHERE ep.event_club_service_id = e.id AND ep.pricing_type = 'TABLE') AS price_table " +

	            sql + " ORDER BY e.start_time ASC LIMIT :limit OFFSET :offset";

	    List<EventPublicResponseDto> results = jdbcTemplate.query(selectSql, params, (rs, rowNum) -> mapRowToDto(rs));

	    // === NEW LOGIC TO POPULATE PRICING CONFIGURATIONS ===
	    if (!results.isEmpty()) {
	        List<Long> eventIds = results.stream().map(EventPublicResponseDto::getId).collect(Collectors.toList());
	        MapSqlParameterSource pricingParams = new MapSqlParameterSource();
	        pricingParams.addValue("eventIds", eventIds);
	        
	        String pricingSql = "SELECT * FROM hogu.event_pricing_configurations WHERE event_club_service_id IN (:eventIds)";
	        
	        List<Map<String, Object>> pricingRows = jdbcTemplate.queryForList(pricingSql, pricingParams);
	        
	        // Group by event_club_service_id
	        Map<Long, List<EventPricingConfigurationResponseDto>> pricingMap = pricingRows.stream()
	            .collect(Collectors.groupingBy(
	                row -> ((Number) row.get("event_club_service_id")).longValue(),
	                Collectors.mapping(row -> {
	                	Object priceObj = row.get("price");
	                	java.math.BigDecimal price = null;
	                	if (priceObj instanceof java.math.BigDecimal) {
	                		price = (java.math.BigDecimal) priceObj;
	                	} else if (priceObj instanceof Number) {
	                		price = java.math.BigDecimal.valueOf(((Number) priceObj).doubleValue());
	                	}
	                	
	                	return EventPricingConfigurationResponseDto.builder()
	                        .id(((Number) row.get("id")).longValue())
	                        .pricingType(PricingType.valueOf((String) row.get("pricing_type")))
	                        .description((String) row.get("description"))
	                        .price(price)
	                        //.capacity((Integer) row.get("capacity")) // Assuming capacity column exists, otherwise omit
	                        //.isActive((Boolean) row.get("is_active")) // Assuming is_active column exists
	                        .build();
	                }, 
	                Collectors.toList())
	            ));
	        
	        // Attach to DTOs
	        for (EventPublicResponseDto dto : results) {
	            dto.setPricingConfigurations(pricingMap.getOrDefault(dto.getId(), Collections.emptyList()));
	        }
	    }
	    // ====================================================

	    return new PageImpl<>(results, pageable, total != null ? total : 0);
	}

	// MAPPER
	private EventPublicResponseDto mapRowToDto(ResultSet rs) throws SQLException {
		// Conversione immagini
		String imagesStr = rs.getString("images");
		List<String> imagesList = new ArrayList<>();
		if (imagesStr != null && !imagesStr.isEmpty()) {
			imagesStr = imagesStr.replace("[", "").replace("]", "");
			if (!imagesStr.trim().isEmpty()) {
				imagesList = Arrays.asList(imagesStr.split(","));
			}
		}

		return EventPublicResponseDto.builder().id(rs.getLong("id")).name(rs.getString("name"))
				.description(rs.getString("description")).startTime(rs.getObject("start_time", OffsetDateTime.class))
				.endTime(rs.getObject("end_time", OffsetDateTime.class)).price(rs.getBigDecimal("price")) // Prezzo base
																											// generico

				// Mappatura nuovi prezzi (usiamo getObject per gestire eventuali NULL)
				.priceMan(rs.getObject("price_man") != null ? rs.getBigDecimal("price_man") : null)
				.priceWoman(rs.getObject("price_woman") != null ? rs.getBigDecimal("price_woman") : null)
				.tableMinPrice(rs.getObject("price_table") != null ? rs.getBigDecimal("price_table") : null)

				.djName(rs.getString("dj_name")).theme(rs.getString("theme")).images(imagesList)
				.clubName(rs.getString("club_name")).city(rs.getString("city")).address(rs.getString("address"))
				.maxCapacity(rs.getObject("max_capacity", Integer.class))
				.clubId(rs.getLong("club_service_id"))
				.build();
	}
}
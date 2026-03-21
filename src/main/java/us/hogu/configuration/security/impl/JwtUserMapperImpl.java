package us.hogu.configuration.security.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Profile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import us.hogu.configuration.security.constants.JwtNameParamsHUConstants;
import us.hogu.configuration.security.dto.UserAccount;
import us.hogu.configuration.security.interfaces.JwtUserMapper;
import us.hogu.model.ServiceLocale;
import us.hogu.model.enums.ServiceType;

@RequiredArgsConstructor
@Component
public class JwtUserMapperImpl implements JwtUserMapper {

	@Override
	public UserAccount mapFromJwtAndHeader(Jwt jwt) {
		String accountId = jwt.getClaimAsString(JwtNameParamsHUConstants.UTSER_ID);
		String email = jwt.getClaimAsString(JwtNameParamsHUConstants.EMAIL);
		String userRole = jwt.getClaimAsString(JwtNameParamsHUConstants.USER_ROLE);
		String name = jwt.getClaimAsString(JwtNameParamsHUConstants.NAME);
		String surname = jwt.getClaimAsString(JwtNameParamsHUConstants.SURNAME);

		List<Map<String, Object>> localesClaim = jwt.getClaim(JwtNameParamsHUConstants.SERVICE_LOCALES);
		List<ServiceLocale> serviceLocales = null;

		if (localesClaim != null) {
			serviceLocales = localesClaim.stream().map(map -> ServiceLocale.builder()
					.id(map.get("id") != null ? ((Number) map.get("id")).longValue() : null)
					.language((String) map.get("language"))
					.country((String) map.get("country"))
					.state((String) map.get("state"))
					.city((String) map.get("city"))
					.address((String) map.get("address"))
					.serviceType(map.get("serviceType") != null ? ServiceType.valueOf((String) map.get("serviceType"))
							: null)
					.build())
					.collect(Collectors.toList());
		}

		return UserAccount.builder()
				.accountId(Long.parseLong(accountId))
				.email(email)
				.name(name)
				.surname(surname)
				.serviceLocales(serviceLocales)
				.authorities(List.of(new SimpleGrantedAuthority("ROLE_" + userRole)))
				.build();
	}

}

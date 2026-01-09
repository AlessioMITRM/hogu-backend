package us.hogu.configuration.security.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Profile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import us.hogu.configuration.security.constants.JwtNameParamsHUConstants;
import us.hogu.configuration.security.dto.UserAccount;
import us.hogu.configuration.security.interfaces.JwtUserMapper;
import us.hogu.configuration.security.util.UserSVUtils;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
@Profile({"dev", "prod"})
public class JwtUserMapperImpl implements JwtUserMapper {
	
	@Override
    public UserAccount mapFromJwtAndHeader(Jwt jwt) {
		String accountId = jwt.getClaimAsString(JwtNameParamsHUConstants.UTSER_ID);
		String email = jwt.getClaimAsString(JwtNameParamsHUConstants.EMAIL);
		String userRole = jwt.getClaimAsString(JwtNameParamsHUConstants.USER_ROLE);
		
		return UserAccount.builder()
		        .accountId(Long.parseLong(accountId))
		        .email(email)
		        .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + userRole)))
		        .build();
	}
	
}

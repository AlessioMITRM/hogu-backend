package us.hogu.configuration.security.interfaces;

import org.springframework.security.oauth2.jwt.Jwt;

import us.hogu.configuration.security.dto.UserAccount;

public interface JwtUserMapper {
	
	UserAccount mapFromJwtAndHeader(Jwt jwt);

}

package us.hogu.service.intefaces;

import org.springframework.security.oauth2.jwt.Jwt;

import us.hogu.configuration.security.dto.UserAccount;

public interface UserAccountAssemblerService {


	UserAccount buildValidatedUserAccount(Jwt jwt);

}

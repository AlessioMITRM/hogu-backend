package us.hogu.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.security.oauth2.jwt.Jwt;

import us.hogu.common.constants.ErrorConstants;
import us.hogu.configuration.security.dto.UserAccount;
import us.hogu.configuration.security.interfaces.JwtUserMapper;
import us.hogu.exception.RoleNotAllowedException;
import us.hogu.service.intefaces.UserAccountAssemblerService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class UserAccountAssemblerServiceImpl implements UserAccountAssemblerService {
    private final JwtUserMapper jwtUserMapper;
    
    
    @Override
    public UserAccount buildValidatedUserAccount(Jwt jwt) {
       
    	return jwtUserMapper.mapFromJwtAndHeader(jwt);
    }
    
}

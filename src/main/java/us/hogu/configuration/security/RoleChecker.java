package us.hogu.configuration.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import us.hogu.configuration.security.util.UserSVUtils;
import us.hogu.model.enums.UserRole;

@Component("roleChecker")
public class RoleChecker {
	
    public boolean isCustomer(Authentication auth) {
        return auth.getAuthorities().stream()
                   .anyMatch(a -> a.getAuthority().equals(UserRole.CUSTOMER));
    }
    
    public boolean isProvider(Authentication auth) {
        return auth.getAuthorities().stream()
                   .anyMatch(a -> a.getAuthority().equals(UserRole.PROVIDER));
    }
    
    public boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream()
                   .anyMatch(a -> a.getAuthority().equals(UserRole.ADMIN));
    }
}
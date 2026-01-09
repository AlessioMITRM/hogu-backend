package us.hogu.configuration.security.dto;

import java.util.List;

import org.springframework.security.core.GrantedAuthority;

import us.hogu.configuration.security.util.UserSVUtils;
import us.hogu.model.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserAccount {
	private long accountId;
	private String email;
    private List<GrantedAuthority> authorities;    
    
 
    public boolean isCustomer() {
        if (authorities == null) return false;

        return authorities.stream()
                .anyMatch(a -> UserSVUtils.withPrefixRole(UserRole.CUSTOMER.name()).equals(a.getAuthority()));
    }

    public boolean isProvider() {
        if (authorities == null) return false;

        return authorities.stream()
                .anyMatch(a -> UserSVUtils.withPrefixRole(UserRole.PROVIDER.name()).equals(a.getAuthority()));
    }
    
    public boolean isAdmin() {
        if (authorities == null) return false;

        return authorities.stream()
                .anyMatch(a -> UserSVUtils.withPrefixRole(UserRole.ADMIN.name()).equals(a.getAuthority()));
    }
    
    public UserRole getRole() {
    	return UserRole.fromName(authorities.get(0).getAuthority());
    }

}

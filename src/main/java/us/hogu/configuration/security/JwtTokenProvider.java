package us.hogu.configuration.security;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import us.hogu.configuration.security.constants.JwtNameParamsHUConstants;
import us.hogu.model.User;

@Component
public class JwtTokenProvider {
    
    @Value("${jwt.secret:mySecretKey123456789012345678901234567890}")
    private String jwtSecret;
    
    @Value("${jwt.expiration:3600000}") // 1 ora in millisecondi
    private long jwtExpiration;
    
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }
    
    public String generateToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);
        
        return Jwts.builder()
            .setSubject(user.getId().toString())
            .claim(JwtNameParamsHUConstants.UTSER_ID, user.getId())
            .claim(JwtNameParamsHUConstants.EMAIL, user.getEmail())
            .claim(JwtNameParamsHUConstants.USER_ROLE, user.getRole().name())
            .claim(JwtNameParamsHUConstants.NAME, user.getName())
            .claim(JwtNameParamsHUConstants.SURNAME, user.getSurname())
            .claim(JwtNameParamsHUConstants.SERVICE_LOCALES, user.getServiceLocales())
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(getSigningKey(), SignatureAlgorithm.HS512)
            .compact();
    }
    
    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
            
        return Long.parseLong(claims.getSubject());
    }
    
    public String getRoleFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
            
        return claims.get("role", String.class);
    }
    
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }
}

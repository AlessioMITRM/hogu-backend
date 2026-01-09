package us.hogu.configuration.security.filter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import us.hogu.common.constants.ErrorConstants;
import us.hogu.configuration.security.dto.UserAccount;
import us.hogu.configuration.security.util.UserSVUtils;
import us.hogu.exception.RoleNotAllowedException;
import us.hogu.service.intefaces.UserAccountAssemblerService;

/**
 * Filtro custom che valida un secondo JWT e associa un ruolo proveniente da un header separato.
 */
@Profile("dev")
@Component
public class DevUserAccountValidationFilter extends OncePerRequestFilter {

    private static final List<String> EXCLUDED_PATHS = List.of(
            "/api/public",
            "/v3/api-docs", 	
            "/v3/api-docs", 
            "/swagger-ui", 
            "/swagger-ui.html", 
            "/swagger", 
            "/webjars"
    );

    private final UserAccountAssemblerService userAccountAssemblerService;
    

    public DevUserAccountValidationFilter(UserAccountAssemblerService userAccountAssemblerService) {
        this.userAccountAssemblerService = userAccountAssemblerService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
    throws ServletException, IOException {

        String path = request.getRequestURI();
        boolean excluded = path.equals("/") || 
        		path.equals("/api/v1/utente/registrazione-controllo") ||
        		EXCLUDED_PATHS.stream().anyMatch(path::startsWith);
        if (excluded) {
            filterChain.doFilter(request, response);
            return;
        }

        // Primo JWT dal SecurityContext
        Authentication principalAuth = SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = null;
        if (principalAuth instanceof JwtAuthenticationToken) {
            JwtAuthenticationToken token = (JwtAuthenticationToken) principalAuth;
            jwt = token.getToken();
        }

        try {
            // Mappa i dati combinati in UserAccount
            UserAccount userAccount = userAccountAssemblerService.buildValidatedUserAccount(jwt);

            // Crea Authentication e sostituisci nel SecurityContext
            Authentication auth = new UsernamePasswordAuthenticationToken(
            	    userAccount,                   
            	    null,                          
            	    userAccount.getAuthorities()   
            	);                
            
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (RoleNotAllowedException ex) {
        	createErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, ex.getMessage());
            return;
        } catch (UsernameNotFoundException ex) {
        	createErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, ex.getMessage());
            return;
        } catch (Exception ex) {
        	createErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, 
        			ErrorConstants.GENERIC_ERROR_AUTHORIZATION.getMessage());
            return;
        }

        filterChain.doFilter(request, response);
    }
    
    private void createErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Map<String, String> body = Map.of("error", message);
        String json = new ObjectMapper().writeValueAsString(body);

        response.getWriter().write(json);
        response.getWriter().flush();
    }

}


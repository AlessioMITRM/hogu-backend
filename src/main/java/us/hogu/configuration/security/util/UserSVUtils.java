package us.hogu.configuration.security.util;

public class UserSVUtils {
    private UserSVUtils() {}
	
    public static final String ROLE_PREFIX = "ROLE_";
    public static final String CODE_CUAA_HEADER = "X-CODE-CUAA";
    public static final String DENOMINATION_CUAA_HEADER = "X-DENOMINATION-CUAA";

    
    /**
     * Restituisce il ruolo completo con il prefisso "ROLE_"
     * @param roleCode codice del ruolo, es. "AGRI"
     * @return ruolo completo, es. "ROLE_AGRI"
     */
    public static String withPrefixRole(String roleCode) {
        if (roleCode == null || roleCode.isEmpty()) {
            throw new IllegalArgumentException("roleCode non può essere null o vuoto");
        }
        
        return ROLE_PREFIX + roleCode;
    }
}

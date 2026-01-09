package us.hogu.model.enums;

public enum UserRole {
    CUSTOMER("CLIENTE", 0),
    PROVIDER("FORNITORE", 1),
    ADMIN("AMMINISTRATORE", 2);
    
    private final String italianValue;
    private final int roleId;

    UserRole(String italianValue, int roleId) {
        this.italianValue = italianValue;
        this.roleId = roleId;
    }
    
    public String getItalianValue() {
        return italianValue;
    }

    public int getRoleId() {
        return roleId;
    }
    
    public static UserRole fromItalianValue(String italianValue) {
        for (UserRole role : values()) {
            if (role.italianValue.equals(italianValue)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Valore italiano non valido: " + italianValue);
    }
    
    public static UserRole fromName(String value) {
        for (UserRole role : values()) {
            if (role.name().equalsIgnoreCase(value)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Valore non valido: " + value);
    }
}

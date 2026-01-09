package us.hogu.model.enums;

public enum LanguageType {
    ITALIAN("it"),
    ENGLISH("en");
    
    private final String value;
    
    LanguageType(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    public static LanguageType fromValue(String value) {
        for (LanguageType status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Valore non valido: " + value);
    }
}

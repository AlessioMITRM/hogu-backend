package us.hogu.model.enums;

public enum ProviderStatus {
	APPROVED("APPROVATO"),
	REJECTED("RIFIUTATO"),
	SUSPENDED("SOSPESO");
    
    private final String italianValue;
    
	ProviderStatus(String italianValue) {
        this.italianValue = italianValue;
    }
    
    public String getItalianValue() {
        return italianValue;
    }
    
    public static ProviderStatus fromItalianValue(String italianValue) {
        for (ProviderStatus type : values()) {
            if (type.italianValue.equals(italianValue)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Valore italiano non valido: " + italianValue);
    }
}

package us.hogu.model.enums;

public enum ServiceType {
    RESTAURANT("RISTORANTE"),
    NCC("NCC"),
    CLUB("CLUB"),
    LUGGAGE("BAGAGLI"),
	BNB("B&B");
    
    private final String italianValue;
    
    ServiceType(String italianValue) {
        this.italianValue = italianValue;
    }
    
    public String getItalianValue() {
        return italianValue;
    }
    
    public static ServiceType fromItalianValue(String italianValue) {
        for (ServiceType type : values()) {
            if (type.italianValue.equals(italianValue)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Valore italiano non valido: " + italianValue);
    }
}

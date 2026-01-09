package us.hogu.model.enums;

public enum BookingAction {
    CANCEL("ANNULLA"),
    MODIFY("MODIFICA"),
    REFUND("RIMBORSA"),
    CONFIRM("CONFERMA"),
    REJECT("RIFIUTA");
    
    private final String italianValue;
    
    BookingAction(String italianValue) {
        this.italianValue = italianValue;
    }
    
    public String getItalianValue() {
        return italianValue;
    }
    
    public static BookingAction fromItalianValue(String italianValue) {
        for (BookingAction action : values()) {
            if (action.italianValue.equals(italianValue)) {
                return action;
            }
        }
        throw new IllegalArgumentException("Valore italiano non valido: " + italianValue);
    }
}
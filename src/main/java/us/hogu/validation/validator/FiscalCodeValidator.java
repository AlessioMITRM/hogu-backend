package us.hogu.validation.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import us.hogu.validation.annotation.FiscalCode;

public class FiscalCodeValidator implements ConstraintValidator<FiscalCode, String> {

    private static final String FISCAL_CODE_REGEX = 
        "^[A-Z]{6}[0-9]{2}[A-Z][0-9]{2}[A-Z][0-9]{3}[A-Z]$";
    private static final String PARTITA_IVA_REGEX =
            "^[0-9]{11}$";

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true; // null/blank lo gestisci con @NotBlank separato se serve
        }
        return value.matches(FISCAL_CODE_REGEX) || value.matches(PARTITA_IVA_REGEX);

    }
}
package us.hogu.validation.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import us.hogu.validation.validator.FiscalCodeValidator;

@Documented
@Constraint(validatedBy = FiscalCodeValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface FiscalCode {

    String message() default "Fiscal Code not valid";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
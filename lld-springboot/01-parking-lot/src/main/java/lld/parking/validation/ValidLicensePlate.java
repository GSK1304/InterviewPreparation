package lld.parking.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = LicensePlateValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidLicensePlate {
    String message() default "Invalid license plate format";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

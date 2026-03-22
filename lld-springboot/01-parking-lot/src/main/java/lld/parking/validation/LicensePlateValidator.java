package lld.parking.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class LicensePlateValidator implements ConstraintValidator<ValidLicensePlate, String> {

    // Indian format: KA01HB1234 or MH02CB5678
    private static final Pattern INDIAN_PLATE  = Pattern.compile("^[A-Z]{2}\\d{2}[A-Z]{1,2}\\d{4}$");
    // International alphanumeric
    private static final Pattern INTERNATIONAL = Pattern.compile("^[A-Z0-9-]{4,15}$");

    @Override
    public boolean isValid(String plate, ConstraintValidatorContext ctx) {
        if (plate == null || plate.isBlank()) return false;
        String upper = plate.toUpperCase().replaceAll("\\s", "");
        return INDIAN_PLATE.matcher(upper).matches() || INTERNATIONAL.matcher(upper).matches();
    }
}

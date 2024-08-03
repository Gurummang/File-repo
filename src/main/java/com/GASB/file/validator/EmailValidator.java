package com.GASB.file.validator;
import com.GASB.file.annotation.ValidEmail;
import com.GASB.file.repository.org.AdminRepo;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
@Component
@RequiredArgsConstructor
public class EmailValidator implements ConstraintValidator<ValidEmail, String>{
    private final AdminRepo adminRepo;

    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {
        boolean isValid = adminRepo.existsByEmail(email);
        if (!isValid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Email does not exist.")
                    .addConstraintViolation();
        }
        return isValid;
    }
}

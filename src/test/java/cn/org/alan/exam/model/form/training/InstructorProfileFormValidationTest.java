package cn.org.alan.exam.model.form.training;

import org.junit.Before;
import org.junit.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class InstructorProfileFormValidationTest {

    private Validator validator;

    @Before
    public void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    public void acceptsValidTeacherBaseInfo() {
        InstructorProfileForm form = new InstructorProfileForm();
        form.setUserId(168);
        form.setOrganization("梧州市人工宝石产业实训中心");
        form.setPosition("智能加工实训讲师");
        form.setPhone("13800138000");
        form.setStatus("ENABLED");

        Set<ConstraintViolation<InstructorProfileForm>> violations = validator.validate(form);

        assertFalse(violations.iterator().hasNext());
    }

    @Test
    public void rejectsOverlongOrganizationAndPosition() {
        InstructorProfileForm form = new InstructorProfileForm();
        form.setUserId(168);
        form.setOrganization(repeat('单', 151));
        form.setPosition(repeat('岗', 101));

        Set<ConstraintViolation<InstructorProfileForm>> violations = validator.validate(form);

        assertTrue(violations.stream().anyMatch(v -> "organization".equals(v.getPropertyPath().toString())));
        assertTrue(violations.stream().anyMatch(v -> "position".equals(v.getPropertyPath().toString())));
    }

    private String repeat(char value, int count) {
        StringBuilder result = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            result.append(value);
        }
        return result.toString();
    }
}

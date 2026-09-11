package cn.org.alan.exam.model.form.user;

import cn.org.alan.exam.common.group.UserGroup;
import org.junit.Before;
import org.junit.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UserFormValidationTest {

    private Validator validator;

    @Before
    public void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    public void updateProfileRejectsInvalidIdentityAndPhone() {
        UserForm form = new UserForm();
        form.setRealName("测试学员");
        form.setIdCard("123456");
        form.setPhone("10086");

        Set<ConstraintViolation<UserForm>> violations =
                validator.validate(form, UserGroup.UpdateProfileGroup.class);

        assertTrue(violations.stream().anyMatch(v -> "idCard".equals(v.getPropertyPath().toString())));
        assertTrue(violations.stream().anyMatch(v -> "phone".equals(v.getPropertyPath().toString())));
    }

    @Test
    public void updateProfileAcceptsValidOptionalProfile() {
        UserForm form = new UserForm();
        form.setRealName("测试学员");
        form.setIdCard("110101199001011234");
        form.setOrganization("人工宝石培训中心");
        form.setPosition("工艺学员");
        form.setPhone("13800138000");

        Set<ConstraintViolation<UserForm>> violations =
                validator.validate(form, UserGroup.UpdateProfileGroup.class);

        assertFalse(violations.iterator().hasNext());
    }
}

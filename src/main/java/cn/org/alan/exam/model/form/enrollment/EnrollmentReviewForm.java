package cn.org.alan.exam.model.form.enrollment;

import lombok.Data;

import javax.validation.constraints.Size;

@Data
public class EnrollmentReviewForm {
    @Size(max = 500, message = "审核说明不能超过500个字符")
    private String reason;
}

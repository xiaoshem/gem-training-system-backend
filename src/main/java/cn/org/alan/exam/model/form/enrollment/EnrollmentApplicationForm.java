package cn.org.alan.exam.model.form.enrollment;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class EnrollmentApplicationForm {
    @NotNull(message = "请选择培训班次")
    private Integer trainingClassId;

    @NotBlank(message = "请填写真实姓名")
    @Size(max = 50, message = "真实姓名不能超过50个字符")
    private String realName;

    @NotBlank(message = "请填写身份证号")
    @Pattern(regexp = "^\\d{17}[0-9Xx]$", message = "身份证号格式不正确")
    private String idCard;

    @Size(max = 100, message = "所在单位不能超过100个字符")
    private String organization;

    @Size(max = 100, message = "岗位不能超过100个字符")
    private String position;

    @NotBlank(message = "请填写联系电话")
    @Pattern(regexp = "^1\\d{10}$", message = "联系电话格式不正确")
    private String phone;
}

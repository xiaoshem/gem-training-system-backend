package cn.org.alan.exam.model.form.training;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class InstructorProfileForm {
    @NotNull(message = "请选择讲师用户")
    private Integer userId;
    @Size(max = 100, message = "职称不能超过100个字符")
    private String professionalTitle;
    @Size(max = 500, message = "擅长领域不能超过500个字符")
    private String specialties;
    @Size(max = 500, message = "资质证书不能超过500个字符")
    private String qualificationCertificate;
    @Size(max = 150, message = "所在企业或单位不能超过150个字符")
    private String organization;
    @Size(max = 100, message = "岗位不能超过100个字符")
    private String position;
    @Pattern(regexp = "(^$)|(^1\\d{10}$)", message = "联系电话格式不正确")
    private String phone;
    private String introduction;
    @Pattern(regexp = "ENABLED|DISABLED", message = "讲师档案状态不正确")
    private String status;
}

package cn.org.alan.exam.model.form.training;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.List;

@Data
public class CourseForm {
    @NotBlank(message = "课程编码不能为空")
    @Size(max = 32, message = "课程编码不能超过32个字符")
    private String courseCode;
    @NotBlank(message = "课程名称不能为空")
    @Size(max = 100, message = "课程名称不能超过100个字符")
    private String courseName;
    @NotBlank(message = "课程类别不能为空")
    @Size(max = 50, message = "课程类别不能超过50个字符")
    private String courseCategory;
    @Size(max = 1000, message = "课程简介不能超过1000个字符")
    private String courseDescription;
    @Size(max = 500, message = "封面地址不能超过500个字符")
    private String coverImage;
    @NotBlank(message = "课程大纲不能为空")
    private String syllabus;
    @NotNull(message = "总课时不能为空")
    @Min(value = 1, message = "总课时必须大于0")
    private Integer totalHours;
    @NotBlank(message = "适用对象不能为空")
    @Size(max = 500, message = "适用对象不能超过500个字符")
    private String targetAudience;
    @Size(max = 1000, message = "前置要求不能超过1000个字符")
    private String prerequisites;
    private Integer defaultInstructorId;
    private List<Integer> instructorIds;
    private String skillIndicators;
    private String scoringRules;
    @Pattern(regexp = "ENABLED|DISABLED", message = "课程状态不正确")
    private String status;
}

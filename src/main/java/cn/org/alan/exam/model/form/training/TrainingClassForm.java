package cn.org.alan.exam.model.form.training;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class TrainingClassForm {
    @NotBlank(message = "班次编码不能为空")
    @Size(max = 32, message = "班次编码不能超过32个字符")
    private String classCode;
    @NotNull(message = "请选择课程")
    private Integer courseId;
    @NotBlank(message = "班次名称不能为空")
    @Size(max = 150, message = "班次名称不能超过150个字符")
    private String className;
    @NotNull(message = "请选择主讲师")
    private Integer instructorId;
    @NotNull(message = "请选择报名开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime enrollmentStart;
    @NotNull(message = "请选择报名结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime enrollmentEnd;
    @NotNull(message = "请选择开课日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    @NotNull(message = "请选择结课日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
    @NotBlank(message = "上课地点不能为空")
    @Size(max = 255, message = "上课地点不能超过255个字符")
    private String location;
    @NotNull(message = "请输入招生名额")
    @Min(value = 1, message = "招生名额必须大于0")
    private Integer capacity;
    @NotNull(message = "请输入培训费用")
    @DecimalMin(value = "0.00", message = "培训费用不能小于0")
    private BigDecimal fee;
}

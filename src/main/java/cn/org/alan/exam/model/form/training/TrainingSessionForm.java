package cn.org.alan.exam.model.form.training;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

@Data
public class TrainingSessionForm {
    @NotNull(message = "培训班次不能为空")
    private Integer trainingClassId;
    @NotNull(message = "课次序号不能为空")
    @Min(value = 1, message = "课次序号必须大于0")
    private Integer sessionNo;
    @NotBlank(message = "课次主题不能为空")
    @Size(max = 200, message = "课次主题不能超过200个字符")
    private String topic;
    @Size(max = 2000, message = "授课内容不能超过2000个字符")
    private String content;
    @NotNull(message = "请选择课次开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
    @NotNull(message = "请选择课次结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
    @NotBlank(message = "课次地点不能为空")
    @Size(max = 255, message = "课次地点不能超过255个字符")
    private String location;
    @NotNull(message = "请选择授课讲师")
    private Integer instructorId;
    @Pattern(regexp = "PLANNED|COMPLETED|CANCELLED", message = "课次状态不正确")
    private String status;
}

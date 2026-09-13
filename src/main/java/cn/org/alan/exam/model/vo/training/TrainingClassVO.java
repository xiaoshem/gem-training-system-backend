package cn.org.alan.exam.model.vo.training;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class TrainingClassVO {
    private Integer id;
    private String classCode;
    private Integer courseId;
    private String courseCode;
    private String courseName;
    private String courseCategory;
    private String courseDescription;
    private String coverImage;
    private String syllabus;
    private Integer totalHours;
    private String targetAudience;
    private String prerequisites;
    private String skillIndicators;
    private String scoringRules;
    private String className;
    private Integer instructorId;
    private String instructorName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime enrollmentStart;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime enrollmentEnd;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
    private String location;
    private Integer capacity;
    private BigDecimal fee;
    private String classStatus;
    private String publishStatus;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishTime;
    private Integer sessionCount;
    private Integer activeSessionCount;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}

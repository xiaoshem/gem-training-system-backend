package cn.org.alan.exam.model.vo.training;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CourseVO {
    private Integer id;
    private String courseCode;
    private String courseName;
    private String courseCategory;
    private String courseDescription;
    private String coverImage;
    private String syllabus;
    private Integer totalHours;
    private String targetAudience;
    private String prerequisites;
    private Integer defaultInstructorId;
    private String defaultInstructorName;
    private List<Integer> instructorIds;
    private String instructorNames;
    private String skillIndicators;
    private String scoringRules;
    private String status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}

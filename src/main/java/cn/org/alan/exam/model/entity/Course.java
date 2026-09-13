package cn.org.alan.exam.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("t_course")
public class Course implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
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
    private String skillIndicators;
    private String scoringRules;
    private String status;
    @TableField(value = "created_by", fill = FieldFill.INSERT)
    private Integer userId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer isDeleted;
}

package cn.org.alan.exam.model.vo.training;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InstructorProfileVO {
    private Integer id;
    private Integer userId;
    private String userName;
    private String realName;
    private String organization;
    private String position;
    private String phone;
    private String professionalTitle;
    private String specialties;
    private String qualificationCertificate;
    private String introduction;
    private String status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}

package cn.org.alan.exam.model.vo.enrollment;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class EnrollmentVO {
    private Integer id;
    private String enrollmentNo;
    private Integer trainingClassId;
    private String classCode;
    private String className;
    private String courseName;
    private Integer studentId;
    private String studentUserName;
    private String realName;
    private String idCard;
    private String organization;
    private String position;
    private String phone;
    private String status;
    private String reviewReason;
    private Integer reviewedBy;
    private String reviewerName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reviewedAt;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
    private String location;
    private BigDecimal fee;
    private Integer paymentOrderId;
    private String orderNo;
    private String paymentStatus;
    private BigDecimal paymentAmount;
    private String transactionNo;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime paidAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}

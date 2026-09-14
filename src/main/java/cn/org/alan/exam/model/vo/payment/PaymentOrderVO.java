package cn.org.alan.exam.model.vo.payment;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentOrderVO {
    private Integer id;
    private String orderNo;
    private Integer enrollmentId;
    private String enrollmentNo;
    private Integer trainingClassId;
    private String classCode;
    private String className;
    private String courseName;
    private Integer studentId;
    private String studentUserName;
    private String studentName;
    private BigDecimal amount;
    private String status;
    private String transactionNo;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime paidAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}

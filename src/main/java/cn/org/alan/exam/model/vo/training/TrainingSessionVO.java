package cn.org.alan.exam.model.vo.training;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TrainingSessionVO {
    private Integer id;
    private Integer trainingClassId;
    private Integer sessionNo;
    private String topic;
    private String content;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
    private String location;
    private Integer instructorId;
    private String instructorName;
    private String status;
}

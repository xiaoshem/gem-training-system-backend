package cn.org.alan.exam.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("t_training_enrollment")
public class TrainingEnrollment implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    private String enrollmentNo;
    private Integer trainingClassId;
    private Integer studentId;
    private String snapshotRealName;
    private String snapshotIdCard;
    private String snapshotOrganization;
    private String snapshotPosition;
    private String snapshotPhone;
    private String status;
    private String reviewReason;
    private Integer reviewedBy;
    private LocalDateTime reviewedAt;
    private LocalDateTime cancelledAt;
    @Version
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer isDeleted;
}

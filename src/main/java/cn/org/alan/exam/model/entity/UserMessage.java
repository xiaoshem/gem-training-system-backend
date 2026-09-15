package cn.org.alan.exam.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("t_user_message")
public class UserMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    private Integer receiverId;
    private String title;
    private String content;
    private String messageType;
    private String businessType;
    private Integer businessId;
    private Integer isRead;
    private LocalDateTime readTime;
    private Integer createdBy;
    private LocalDateTime createTime;
    @TableLogic
    private Integer isDeleted;
}

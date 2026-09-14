package cn.org.alan.exam.service;

import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.model.entity.UserMessage;
import cn.org.alan.exam.model.vo.message.UserMessageVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

public interface IUserMessageService extends IService<UserMessage> {
    Result<IPage<UserMessageVO>> paging(Integer pageNum, Integer pageSize, Boolean unreadOnly);
    Result<Integer> unreadCount();
    Result<String> read(Integer id);
    Result<String> readAll();
}

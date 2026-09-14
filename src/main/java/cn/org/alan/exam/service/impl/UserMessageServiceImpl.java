package cn.org.alan.exam.service.impl;

import cn.org.alan.exam.common.exception.ServiceRuntimeException;
import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.mapper.UserMessageMapper;
import cn.org.alan.exam.model.entity.UserMessage;
import cn.org.alan.exam.model.vo.message.UserMessageVO;
import cn.org.alan.exam.service.IUserMessageService;
import cn.org.alan.exam.utils.SecurityUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserMessageServiceImpl extends ServiceImpl<UserMessageMapper, UserMessage>
        implements IUserMessageService {
    @Override
    public Result<IPage<UserMessageVO>> paging(Integer pageNum, Integer pageSize, Boolean unreadOnly) {
        LambdaQueryWrapper<UserMessage> query = new LambdaQueryWrapper<UserMessage>()
                .eq(UserMessage::getReceiverId, SecurityUtil.getUserId())
                .eq(Boolean.TRUE.equals(unreadOnly), UserMessage::getIsRead, 0)
                .orderByDesc(UserMessage::getCreateTime)
                .orderByDesc(UserMessage::getId);
        Page<UserMessage> entityPage = page(new Page<>(pageNum, pageSize), query);
        List<UserMessageVO> records = entityPage.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        Page<UserMessageVO> result = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        result.setRecords(records);
        return Result.success("查询成功", result);
    }

    @Override
    public Result<Integer> unreadCount() {
        long count = count(new LambdaQueryWrapper<UserMessage>()
                .eq(UserMessage::getReceiverId, SecurityUtil.getUserId())
                .eq(UserMessage::getIsRead, 0));
        return Result.success("查询成功", Math.toIntExact(count));
    }

    @Override
    @Transactional
    public Result<String> read(Integer id) {
        UserMessage message = getById(id);
        if (message == null || !SecurityUtil.getUserId().equals(message.getReceiverId())) {
            throw new ServiceRuntimeException("消息不存在");
        }
        if (!Integer.valueOf(1).equals(message.getIsRead())) {
            message.setIsRead(1);
            message.setReadTime(LocalDateTime.now());
            updateById(message);
        }
        return Result.success("消息已读");
    }

    @Override
    @Transactional
    public Result<String> readAll() {
        List<UserMessage> messages = list(new LambdaQueryWrapper<UserMessage>()
                .eq(UserMessage::getReceiverId, SecurityUtil.getUserId())
                .eq(UserMessage::getIsRead, 0));
        LocalDateTime now = LocalDateTime.now();
        for (UserMessage message : messages) {
            message.setIsRead(1);
            message.setReadTime(now);
            updateById(message);
        }
        return Result.success("全部消息已标记为已读");
    }

    private UserMessageVO toVO(UserMessage message) {
        UserMessageVO vo = new UserMessageVO();
        BeanUtils.copyProperties(message, vo);
        return vo;
    }
}

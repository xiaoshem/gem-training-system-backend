package cn.org.alan.exam.controller;

import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.model.vo.message.UserMessageVO;
import cn.org.alan.exam.service.IUserMessageService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@Api(tags = "站内消息")
@RestController
@RequestMapping("/api/user-messages")
@PreAuthorize("isAuthenticated()")
public class UserMessageController {
    @Resource
    private IUserMessageService userMessageService;

    @ApiOperation("分页查询我的消息")
    @GetMapping("/paging")
    public Result<IPage<UserMessageVO>> paging(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "unreadOnly", defaultValue = "false") Boolean unreadOnly) {
        return userMessageService.paging(pageNum, pageSize, unreadOnly);
    }

    @ApiOperation("查询未读消息数量")
    @GetMapping("/unread-count")
    public Result<Integer> unreadCount() {
        return userMessageService.unreadCount();
    }

    @ApiOperation("标记单条消息已读")
    @PutMapping("/{id}/read")
    public Result<String> read(@PathVariable Integer id) {
        return userMessageService.read(id);
    }

    @ApiOperation("全部标记已读")
    @PutMapping("/read-all")
    public Result<String> readAll() {
        return userMessageService.readAll();
    }
}

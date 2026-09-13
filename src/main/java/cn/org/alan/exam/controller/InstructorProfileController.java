package cn.org.alan.exam.controller;

import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.model.form.training.InstructorProfileForm;
import cn.org.alan.exam.model.vo.training.InstructorProfileVO;
import cn.org.alan.exam.service.IInstructorProfileService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@Api(tags = "培训讲师档案管理")
@RestController
@RequestMapping("/api/instructors")
@PreAuthorize("hasAnyAuthority('role_admin')")
public class InstructorProfileController {
    @Resource
    private IInstructorProfileService instructorProfileService;

    @ApiOperation("分页查询讲师档案")
    @GetMapping("/paging")
    public Result<IPage<InstructorProfileVO>> paging(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) String status) {
        return instructorProfileService.paging(pageNum, pageSize, keyword, status);
    }

    @ApiOperation("可用讲师选项")
    @GetMapping("/options")
    public Result<List<InstructorProfileVO>> options() {
        return instructorProfileService.options();
    }

    @ApiOperation("讲师用户候选项")
    @GetMapping("/candidates")
    public Result<List<InstructorProfileVO>> candidates() {
        return instructorProfileService.candidates();
    }

    @ApiOperation("新增讲师档案")
    @PostMapping
    public Result<String> add(@Validated @RequestBody InstructorProfileForm form) {
        return instructorProfileService.add(form);
    }

    @ApiOperation("修改讲师档案")
    @PutMapping("/{id}")
    public Result<String> update(@PathVariable Integer id, @Validated @RequestBody InstructorProfileForm form) {
        return instructorProfileService.update(id, form);
    }

    @ApiOperation("删除讲师档案")
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Integer id) {
        return instructorProfileService.delete(id);
    }
}

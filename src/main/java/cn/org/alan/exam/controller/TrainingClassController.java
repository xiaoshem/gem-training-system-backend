package cn.org.alan.exam.controller;

import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.model.form.training.TrainingClassForm;
import cn.org.alan.exam.model.vo.training.TrainingClassVO;
import cn.org.alan.exam.service.ITrainingClassService;
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

@Api(tags = "培训班次管理")
@RestController
@RequestMapping("/api/training-classes")
public class TrainingClassController {
    @Resource
    private ITrainingClassService trainingClassService;

    @ApiOperation("管理端分页查询培训班次")
    @GetMapping("/paging")
    @PreAuthorize("hasAnyAuthority('role_admin','role_teacher')")
    public Result<IPage<TrainingClassVO>> paging(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "classStatus", required = false) String classStatus,
            @RequestParam(value = "publishStatus", required = false) String publishStatus) {
        return trainingClassService.paging(pageNum, pageSize, keyword, classStatus, publishStatus);
    }

    @ApiOperation("学员查询已发布培训班次")
    @GetMapping("/available")
    @PreAuthorize("hasAnyAuthority('role_student','role_teacher','role_admin')")
    public Result<IPage<TrainingClassVO>> available(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return trainingClassService.available(pageNum, pageSize, keyword);
    }

    @ApiOperation("查询培训班次详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('role_student','role_teacher','role_admin')")
    public Result<TrainingClassVO> detail(@PathVariable Integer id) {
        return trainingClassService.detail(id);
    }

    @ApiOperation("新增培训班次")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('role_admin')")
    public Result<String> add(@Validated @RequestBody TrainingClassForm form) {
        return trainingClassService.add(form);
    }

    @ApiOperation("修改培训班次")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('role_admin')")
    public Result<String> update(@PathVariable Integer id, @Validated @RequestBody TrainingClassForm form) {
        return trainingClassService.update(id, form);
    }

    @ApiOperation("删除培训班次")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('role_admin')")
    public Result<String> delete(@PathVariable Integer id) {
        return trainingClassService.delete(id);
    }

    @ApiOperation("发布培训班次")
    @PutMapping("/{id}/publish")
    @PreAuthorize("hasAnyAuthority('role_admin')")
    public Result<String> publish(@PathVariable Integer id) {
        return trainingClassService.publish(id);
    }

    @ApiOperation("撤回培训班次")
    @PutMapping("/{id}/unpublish")
    @PreAuthorize("hasAnyAuthority('role_admin')")
    public Result<String> unpublish(@PathVariable Integer id) {
        return trainingClassService.unpublish(id);
    }

    @ApiOperation("取消培训班次")
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('role_admin')")
    public Result<String> cancel(@PathVariable Integer id) {
        return trainingClassService.cancel(id);
    }
}

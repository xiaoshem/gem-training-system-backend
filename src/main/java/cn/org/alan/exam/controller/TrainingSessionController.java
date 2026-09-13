package cn.org.alan.exam.controller;

import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.model.form.training.TrainingSessionForm;
import cn.org.alan.exam.model.vo.training.TrainingSessionVO;
import cn.org.alan.exam.service.ITrainingSessionService;
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
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@Api(tags = "培训课次管理")
@RestController
@RequestMapping("/api/training-sessions")
public class TrainingSessionController {
    @Resource
    private ITrainingSessionService trainingSessionService;

    @ApiOperation("查询班次课次")
    @GetMapping("/class/{trainingClassId}")
    @PreAuthorize("hasAnyAuthority('role_student','role_teacher','role_admin')")
    public Result<List<TrainingSessionVO>> listByClass(@PathVariable Integer trainingClassId) {
        return trainingSessionService.listByClass(trainingClassId);
    }

    @ApiOperation("新增课次")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('role_admin')")
    public Result<String> add(@Validated @RequestBody TrainingSessionForm form) {
        return trainingSessionService.add(form);
    }

    @ApiOperation("修改课次")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('role_admin')")
    public Result<String> update(@PathVariable Integer id, @Validated @RequestBody TrainingSessionForm form) {
        return trainingSessionService.update(id, form);
    }

    @ApiOperation("删除课次")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('role_admin')")
    public Result<String> delete(@PathVariable Integer id) {
        return trainingSessionService.delete(id);
    }
}

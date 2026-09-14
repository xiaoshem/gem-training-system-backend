package cn.org.alan.exam.controller;

import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.model.form.enrollment.EnrollmentApplicationForm;
import cn.org.alan.exam.model.form.enrollment.EnrollmentReviewForm;
import cn.org.alan.exam.model.vo.enrollment.EnrollmentVO;
import cn.org.alan.exam.service.IEnrollmentService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@Api(tags = "培训报名与审核")
@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {
    @Resource
    private IEnrollmentService enrollmentService;

    @ApiOperation("学员提交培训报名")
    @PostMapping
    @PreAuthorize("hasAuthority('role_student')")
    public Result<String> apply(@Validated @RequestBody EnrollmentApplicationForm form) {
        return enrollmentService.apply(form);
    }

    @ApiOperation("学员分页查询我的报名")
    @GetMapping("/mine/paging")
    @PreAuthorize("hasAuthority('role_student')")
    public Result<IPage<EnrollmentVO>> mine(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) String status) {
        return enrollmentService.mine(pageNum, pageSize, keyword, status);
    }

    @ApiOperation("学员查询指定班次的有效报名")
    @GetMapping("/mine/class/{trainingClassId}")
    @PreAuthorize("hasAuthority('role_student')")
    public Result<EnrollmentVO> mineByClass(@PathVariable Integer trainingClassId) {
        return enrollmentService.mineByClass(trainingClassId);
    }

    @ApiOperation("管理员分页查询报名")
    @GetMapping("/manage/paging")
    @PreAuthorize("hasAuthority('role_admin')")
    public Result<IPage<EnrollmentVO>> manage(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "trainingClassId", required = false) Integer trainingClassId) {
        return enrollmentService.manage(pageNum, pageSize, keyword, status, trainingClassId);
    }

    @ApiOperation("管理员录取报名")
    @PutMapping("/{id}/admit")
    @PreAuthorize("hasAuthority('role_admin')")
    public Result<String> admit(@PathVariable Integer id,
                                @Validated @RequestBody(required = false) EnrollmentReviewForm form) {
        return enrollmentService.admit(id, form);
    }

    @ApiOperation("管理员拒绝报名")
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('role_admin')")
    public Result<String> reject(@PathVariable Integer id,
                                 @Validated @RequestBody EnrollmentReviewForm form) {
        return enrollmentService.reject(id, form);
    }

    @ApiOperation("学员取消报名")
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('role_student')")
    public Result<String> cancel(@PathVariable Integer id) {
        return enrollmentService.cancel(id);
    }
}

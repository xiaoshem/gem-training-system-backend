package cn.org.alan.exam.controller;

import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.model.form.training.CourseForm;
import cn.org.alan.exam.model.vo.training.CourseVO;
import cn.org.alan.exam.service.ICourseService;
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

@Api(tags = "培训课程管理")
@RestController
@RequestMapping("/api/courses")
public class CourseController {
    @Resource
    private ICourseService courseService;

    @ApiOperation("分页查询课程")
    @GetMapping("/paging")
    @PreAuthorize("hasAnyAuthority('role_admin','role_teacher')")
    public Result<IPage<CourseVO>> paging(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) String status) {
        return courseService.paging(pageNum, pageSize, keyword, status);
    }

    @ApiOperation("查询课程详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('role_admin','role_teacher')")
    public Result<CourseVO> detail(@PathVariable Integer id) {
        return courseService.detail(id);
    }

    @ApiOperation("查询可用课程选项")
    @GetMapping("/options")
    @PreAuthorize("hasAnyAuthority('role_admin')")
    public Result<List<CourseVO>> options() {
        return courseService.options();
    }

    @ApiOperation("新增课程")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('role_admin')")
    public Result<String> add(@Validated @RequestBody CourseForm form) {
        return courseService.add(form);
    }

    @ApiOperation("修改课程")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('role_admin')")
    public Result<String> update(@PathVariable Integer id, @Validated @RequestBody CourseForm form) {
        return courseService.update(id, form);
    }

    @ApiOperation("删除课程")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('role_admin')")
    public Result<String> delete(@PathVariable Integer id) {
        return courseService.delete(id);
    }
}

package cn.org.alan.exam.controller;

import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.model.vo.training.TrainingClassVO;
import cn.org.alan.exam.model.vo.training.TrainingSessionVO;
import cn.org.alan.exam.service.ITrainingClassService;
import cn.org.alan.exam.service.ITrainingSessionService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@Api(tags = "公开培训信息")
@RestController
@RequestMapping("/api/public/training-classes")
public class PublicTrainingController {
    @Resource
    private ITrainingClassService trainingClassService;
    @Resource
    private ITrainingSessionService trainingSessionService;

    @ApiOperation("公开查询已发布培训班次")
    @GetMapping
    public Result<IPage<TrainingClassVO>> paging(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return trainingClassService.available(pageNum, pageSize, keyword);
    }

    @ApiOperation("公开查询已发布培训班次详情")
    @GetMapping("/{id}")
    public Result<TrainingClassVO> detail(@PathVariable Integer id) {
        return trainingClassService.publicDetail(id);
    }

    @ApiOperation("公开查询已发布培训班次课次")
    @GetMapping("/{id}/sessions")
    public Result<List<TrainingSessionVO>> sessions(@PathVariable Integer id) {
        return trainingSessionService.publicListByClass(id);
    }
}

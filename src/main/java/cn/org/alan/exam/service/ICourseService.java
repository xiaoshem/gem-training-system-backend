package cn.org.alan.exam.service;

import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.model.entity.Course;
import cn.org.alan.exam.model.form.training.CourseForm;
import cn.org.alan.exam.model.vo.training.CourseVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface ICourseService extends IService<Course> {
    Result<IPage<CourseVO>> paging(Integer pageNum, Integer pageSize, String keyword, String status);
    Result<CourseVO> detail(Integer id);
    Result<List<CourseVO>> options();
    Result<String> add(CourseForm form);
    Result<String> update(Integer id, CourseForm form);
    Result<String> delete(Integer id);
}

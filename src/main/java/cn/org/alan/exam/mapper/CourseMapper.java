package cn.org.alan.exam.mapper;

import cn.org.alan.exam.model.entity.Course;
import cn.org.alan.exam.model.vo.training.CourseVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CourseMapper extends BaseMapper<Course> {
    Page<CourseVO> selectCoursePage(Page<CourseVO> page,
                                    @Param("keyword") String keyword,
                                    @Param("status") String status);

    CourseVO selectCourseVOById(@Param("id") Integer id);

    List<CourseVO> selectEnabledOptions();
}

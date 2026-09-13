package cn.org.alan.exam.mapper;

import cn.org.alan.exam.model.entity.CourseInstructor;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface CourseInstructorMapper extends BaseMapper<CourseInstructor> {
    @Select("select instructor_id from t_course_instructor where course_id = #{courseId} order by id")
    List<Integer> selectInstructorIds(@Param("courseId") Integer courseId);
}

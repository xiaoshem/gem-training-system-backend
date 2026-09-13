package cn.org.alan.exam.service.impl;

import cn.org.alan.exam.common.exception.ServiceRuntimeException;
import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.mapper.CourseInstructorMapper;
import cn.org.alan.exam.mapper.CourseMapper;
import cn.org.alan.exam.mapper.InstructorProfileMapper;
import cn.org.alan.exam.mapper.TrainingClassMapper;
import cn.org.alan.exam.mapper.UserMapper;
import cn.org.alan.exam.model.entity.Course;
import cn.org.alan.exam.model.entity.CourseInstructor;
import cn.org.alan.exam.model.entity.InstructorProfile;
import cn.org.alan.exam.model.entity.TrainingClass;
import cn.org.alan.exam.model.entity.User;
import cn.org.alan.exam.model.enums.CourseStatus;
import cn.org.alan.exam.model.form.training.CourseForm;
import cn.org.alan.exam.model.vo.training.CourseVO;
import cn.org.alan.exam.service.ICourseService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class CourseServiceImpl extends ServiceImpl<CourseMapper, Course> implements ICourseService {
    @Resource
    private CourseMapper courseMapper;
    @Resource
    private CourseInstructorMapper courseInstructorMapper;
    @Resource
    private TrainingClassMapper trainingClassMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private InstructorProfileMapper instructorProfileMapper;

    @Override
    public Result<IPage<CourseVO>> paging(Integer pageNum, Integer pageSize, String keyword, String status) {
        Page<CourseVO> page = courseMapper.selectCoursePage(new Page<>(pageNum, pageSize), keyword, status);
        page.getRecords().forEach(this::fillInstructorIds);
        return Result.success("查询成功", page);
    }

    @Override
    public Result<CourseVO> detail(Integer id) {
        CourseVO course = courseMapper.selectCourseVOById(id);
        if (course == null) {
            throw new ServiceRuntimeException("课程不存在");
        }
        fillInstructorIds(course);
        return Result.success("查询成功", course);
    }

    @Override
    public Result<List<CourseVO>> options() {
        return Result.success("查询成功", courseMapper.selectEnabledOptions());
    }

    @Override
    @Transactional
    public Result<String> add(CourseForm form) {
        validateStatus(form.getStatus());
        ensureCourseCodeUnique(form.getCourseCode(), null);
        Course course = new Course();
        BeanUtils.copyProperties(form, course);
        course.setCourseCode(form.getCourseCode().trim());
        course.setCourseName(form.getCourseName().trim());
        course.setStatus(blank(form.getStatus()) ? CourseStatus.ENABLED.name() : form.getStatus());
        course.setUpdateTime(LocalDateTime.now());
        if (courseMapper.insert(course) == 0) {
            throw new ServiceRuntimeException("新增课程失败");
        }
        saveInstructorRelations(course.getId(), form.getDefaultInstructorId(), form.getInstructorIds());
        return Result.success("新增课程成功");
    }

    @Override
    @Transactional
    public Result<String> update(Integer id, CourseForm form) {
        Course original = requireCourse(id);
        validateStatus(form.getStatus());
        ensureCourseCodeUnique(form.getCourseCode(), id);
        Course course = new Course();
        BeanUtils.copyProperties(form, course);
        course.setId(id);
        course.setCourseCode(form.getCourseCode().trim());
        course.setCourseName(form.getCourseName().trim());
        course.setStatus(blank(form.getStatus()) ? original.getStatus() : form.getStatus());
        course.setUpdateTime(LocalDateTime.now());
        if (courseMapper.updateById(course) == 0) {
            throw new ServiceRuntimeException("修改课程失败");
        }
        saveInstructorRelations(id, form.getDefaultInstructorId(), form.getInstructorIds());
        return Result.success("修改课程成功");
    }

    @Override
    @Transactional
    public Result<String> delete(Integer id) {
        requireCourse(id);
        Long classCount = trainingClassMapper.selectCount(new LambdaQueryWrapper<TrainingClass>()
                .eq(TrainingClass::getCourseId, id));
        if (classCount != null && classCount > 0) {
            throw new ServiceRuntimeException("课程已创建培训班次，不能删除");
        }
        courseInstructorMapper.delete(new LambdaQueryWrapper<CourseInstructor>()
                .eq(CourseInstructor::getCourseId, id));
        if (courseMapper.deleteById(id) == 0) {
            throw new ServiceRuntimeException("删除课程失败");
        }
        return Result.success("删除课程成功");
    }

    private Course requireCourse(Integer id) {
        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw new ServiceRuntimeException("课程不存在");
        }
        return course;
    }

    private void ensureCourseCodeUnique(String code, Integer excludeId) {
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<Course>()
                .eq(Course::getCourseCode, code.trim());
        if (excludeId != null) {
            wrapper.ne(Course::getId, excludeId);
        }
        if (courseMapper.selectCount(wrapper) > 0) {
            throw new ServiceRuntimeException("课程编码已存在");
        }
    }

    private void saveInstructorRelations(Integer courseId, Integer defaultInstructorId, List<Integer> instructorIds) {
        Set<Integer> uniqueIds = new LinkedHashSet<>();
        if (instructorIds != null) {
            uniqueIds.addAll(instructorIds);
        }
        if (defaultInstructorId != null) {
            uniqueIds.add(defaultInstructorId);
        }
        uniqueIds.remove(null);
        for (Integer instructorId : uniqueIds) {
            requireInstructorUser(instructorId);
        }
        courseInstructorMapper.delete(new LambdaQueryWrapper<CourseInstructor>()
                .eq(CourseInstructor::getCourseId, courseId));
        for (Integer instructorId : uniqueIds) {
            CourseInstructor relation = new CourseInstructor();
            relation.setCourseId(courseId);
            relation.setInstructorId(instructorId);
            relation.setCreateTime(LocalDateTime.now());
            courseInstructorMapper.insert(relation);
        }
    }

    private User requireInstructorUser(Integer userId) {
        User user = userMapper.selectById(userId);
        if (user == null || !Integer.valueOf(2).equals(user.getRoleId()) || !Integer.valueOf(1).equals(user.getStatus())) {
            throw new ServiceRuntimeException("所选讲师不存在或已停用");
        }
        Long profileCount = instructorProfileMapper.selectCount(new LambdaQueryWrapper<InstructorProfile>()
                .eq(InstructorProfile::getUserId, userId)
                .eq(InstructorProfile::getStatus, CourseStatus.ENABLED.name()));
        if (profileCount == null || profileCount == 0) {
            throw new ServiceRuntimeException("所选讲师尚未建立有效讲师档案");
        }
        return user;
    }

    private void fillInstructorIds(CourseVO course) {
        List<Integer> ids = courseInstructorMapper.selectInstructorIds(course.getId());
        course.setInstructorIds(ids == null ? new ArrayList<>() : ids);
    }

    private void validateStatus(String status) {
        if (!blank(status) && !CourseStatus.supports(status)) {
            throw new ServiceRuntimeException("课程状态不正确");
        }
    }

    private boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

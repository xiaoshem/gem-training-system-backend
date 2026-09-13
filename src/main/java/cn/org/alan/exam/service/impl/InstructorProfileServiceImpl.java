package cn.org.alan.exam.service.impl;

import cn.org.alan.exam.common.exception.ServiceRuntimeException;
import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.mapper.CourseInstructorMapper;
import cn.org.alan.exam.mapper.CourseMapper;
import cn.org.alan.exam.mapper.InstructorProfileMapper;
import cn.org.alan.exam.mapper.TrainingClassMapper;
import cn.org.alan.exam.mapper.TrainingSessionMapper;
import cn.org.alan.exam.mapper.UserMapper;
import cn.org.alan.exam.model.entity.Course;
import cn.org.alan.exam.model.entity.CourseInstructor;
import cn.org.alan.exam.model.entity.InstructorProfile;
import cn.org.alan.exam.model.entity.TrainingClass;
import cn.org.alan.exam.model.entity.TrainingSession;
import cn.org.alan.exam.model.entity.User;
import cn.org.alan.exam.model.enums.CourseStatus;
import cn.org.alan.exam.model.form.training.InstructorProfileForm;
import cn.org.alan.exam.model.vo.training.InstructorProfileVO;
import cn.org.alan.exam.service.IInstructorProfileService;
import cn.org.alan.exam.utils.SecurityUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class InstructorProfileServiceImpl extends ServiceImpl<InstructorProfileMapper, InstructorProfile>
        implements IInstructorProfileService {
    @Resource
    private InstructorProfileMapper instructorProfileMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private CourseMapper courseMapper;
    @Resource
    private CourseInstructorMapper courseInstructorMapper;
    @Resource
    private TrainingClassMapper trainingClassMapper;
    @Resource
    private TrainingSessionMapper trainingSessionMapper;

    @Override
    public Result<IPage<InstructorProfileVO>> paging(Integer pageNum, Integer pageSize, String keyword, String status) {
        return Result.success("查询成功",
                instructorProfileMapper.selectInstructorPage(new Page<>(pageNum, pageSize), keyword, status));
    }

    @Override
    public Result<List<InstructorProfileVO>> options() {
        return Result.success("查询成功", instructorProfileMapper.selectEnabledOptions());
    }

    @Override
    public Result<List<InstructorProfileVO>> candidates() {
        return Result.success("查询成功", instructorProfileMapper.selectTeacherCandidates());
    }

    @Override
    @Transactional
    public Result<String> add(InstructorProfileForm form) {
        requireTeacher(form.getUserId());
        Long count = instructorProfileMapper.selectCount(new LambdaQueryWrapper<InstructorProfile>()
                .eq(InstructorProfile::getUserId, form.getUserId()));
        if (count != null && count > 0) {
            throw new ServiceRuntimeException("该用户已经建立讲师档案");
        }
        InstructorProfile profile = new InstructorProfile();
        BeanUtils.copyProperties(form, profile);
        profile.setStatus(blank(form.getStatus()) ? CourseStatus.ENABLED.name() : form.getStatus());
        profile.setCreatedBy(SecurityUtil.getUserId());
        profile.setUpdateTime(LocalDateTime.now());
        if (instructorProfileMapper.insert(profile) == 0) {
            throw new ServiceRuntimeException("新增讲师档案失败");
        }
        updateTeacherBaseInfo(form.getUserId(), form.getOrganization(), form.getPosition(), form.getPhone());
        return Result.success("新增讲师档案成功");
    }

    @Override
    @Transactional
    public Result<String> update(Integer id, InstructorProfileForm form) {
        InstructorProfile original = requireProfile(id);
        requireTeacher(form.getUserId());
        Long count = instructorProfileMapper.selectCount(new LambdaQueryWrapper<InstructorProfile>()
                .eq(InstructorProfile::getUserId, form.getUserId())
                .ne(InstructorProfile::getId, id));
        if (count != null && count > 0) {
            throw new ServiceRuntimeException("该用户已经建立讲师档案");
        }
        InstructorProfile profile = new InstructorProfile();
        BeanUtils.copyProperties(form, profile);
        profile.setId(id);
        profile.setStatus(blank(form.getStatus()) ? original.getStatus() : form.getStatus());
        profile.setUpdateTime(LocalDateTime.now());
        if (instructorProfileMapper.updateById(profile) == 0) {
            throw new ServiceRuntimeException("修改讲师档案失败");
        }
        updateTeacherBaseInfo(form.getUserId(), form.getOrganization(), form.getPosition(), form.getPhone());
        return Result.success("修改讲师档案成功");
    }

    @Override
    @Transactional
    public Result<String> delete(Integer id) {
        InstructorProfile profile = requireProfile(id);
        Integer userId = profile.getUserId();
        long references = courseMapper.selectCount(new LambdaQueryWrapper<Course>()
                .eq(Course::getDefaultInstructorId, userId));
        references += courseInstructorMapper.selectCount(new LambdaQueryWrapper<CourseInstructor>()
                .eq(CourseInstructor::getInstructorId, userId));
        references += trainingClassMapper.selectCount(new LambdaQueryWrapper<TrainingClass>()
                .eq(TrainingClass::getInstructorId, userId));
        references += trainingSessionMapper.selectCount(new LambdaQueryWrapper<TrainingSession>()
                .eq(TrainingSession::getInstructorId, userId));
        if (references > 0) {
            throw new ServiceRuntimeException("讲师已关联课程或培训安排，不能删除");
        }
        if (instructorProfileMapper.deleteById(id) == 0) {
            throw new ServiceRuntimeException("删除讲师档案失败");
        }
        return Result.success("删除讲师档案成功");
    }

    private InstructorProfile requireProfile(Integer id) {
        InstructorProfile profile = instructorProfileMapper.selectById(id);
        if (profile == null) {
            throw new ServiceRuntimeException("讲师档案不存在");
        }
        return profile;
    }

    private User requireTeacher(Integer userId) {
        User user = userMapper.selectById(userId);
        if (user == null || !Integer.valueOf(2).equals(user.getRoleId()) || !Integer.valueOf(1).equals(user.getStatus())) {
            throw new ServiceRuntimeException("所选用户不是可用的培训讲师");
        }
        return user;
    }

    private boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void updateTeacherBaseInfo(Integer userId, String organization, String position, String phone) {
        if (organization == null && position == null && phone == null) {
            return;
        }
        User update = new User();
        update.setId(userId);
        if (organization != null) {
            update.setOrganization(organization.trim());
        }
        if (position != null) {
            update.setPosition(position.trim());
        }
        if (phone != null) {
            update.setPhone(phone.trim());
        }
        if (userMapper.updateById(update) == 0) {
            throw new ServiceRuntimeException("更新讲师单位、岗位或联系电话失败");
        }
    }
}

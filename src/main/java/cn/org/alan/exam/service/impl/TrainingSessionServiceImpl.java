package cn.org.alan.exam.service.impl;

import cn.org.alan.exam.common.exception.ServiceRuntimeException;
import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.mapper.TrainingClassMapper;
import cn.org.alan.exam.mapper.TrainingSessionMapper;
import cn.org.alan.exam.mapper.UserMapper;
import cn.org.alan.exam.mapper.CourseMapper;
import cn.org.alan.exam.mapper.InstructorProfileMapper;
import cn.org.alan.exam.model.entity.Course;
import cn.org.alan.exam.model.entity.InstructorProfile;
import cn.org.alan.exam.model.entity.TrainingClass;
import cn.org.alan.exam.model.entity.TrainingSession;
import cn.org.alan.exam.model.entity.User;
import cn.org.alan.exam.model.enums.TrainingClassStatus;
import cn.org.alan.exam.model.enums.CourseStatus;
import cn.org.alan.exam.model.enums.TrainingPublishStatus;
import cn.org.alan.exam.model.enums.TrainingSessionStatus;
import cn.org.alan.exam.model.form.training.TrainingSessionForm;
import cn.org.alan.exam.model.vo.training.TrainingSessionVO;
import cn.org.alan.exam.service.ITrainingSessionService;
import cn.org.alan.exam.utils.SecurityUtil;
import cn.org.alan.exam.utils.TrainingValidationUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TrainingSessionServiceImpl extends ServiceImpl<TrainingSessionMapper, TrainingSession>
        implements ITrainingSessionService {
    @Resource
    private TrainingSessionMapper trainingSessionMapper;
    @Resource
    private TrainingClassMapper trainingClassMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private CourseMapper courseMapper;
    @Resource
    private InstructorProfileMapper instructorProfileMapper;

    @Override
    public Result<List<TrainingSessionVO>> listByClass(Integer trainingClassId) {
        TrainingClass trainingClass = requireClass(trainingClassId);
        ensureReadPermission(trainingClass);
        return Result.success("查询成功", trainingSessionMapper.selectByTrainingClassId(trainingClassId));
    }

    @Override
    public Result<List<TrainingSessionVO>> publicListByClass(Integer trainingClassId) {
        TrainingClass trainingClass = requireClass(trainingClassId);
        ensurePubliclyVisible(trainingClass);
        return Result.success("查询成功", trainingSessionMapper.selectByTrainingClassId(trainingClassId));
    }

    @Override
    @Transactional
    public Result<String> add(TrainingSessionForm form) {
        TrainingClass trainingClass = requireEditableClass(form.getTrainingClassId());
        validate(form, trainingClass, null);
        TrainingSession session = new TrainingSession();
        BeanUtils.copyProperties(form, session);
        session.setTopic(form.getTopic().trim());
        session.setLocation(form.getLocation().trim());
        session.setStatus(blank(form.getStatus()) ? TrainingSessionStatus.PLANNED.name() : form.getStatus());
        session.setUpdateTime(LocalDateTime.now());
        if (trainingSessionMapper.insert(session) == 0) {
            throw new ServiceRuntimeException("新增课次失败");
        }
        return Result.success("新增课次成功");
    }

    @Override
    @Transactional
    public Result<String> update(Integer id, TrainingSessionForm form) {
        TrainingSession original = trainingSessionMapper.selectById(id);
        if (original == null) {
            throw new ServiceRuntimeException("课次不存在");
        }
        TrainingClass trainingClass = requireEditableClass(form.getTrainingClassId());
        validate(form, trainingClass, id);
        TrainingSession session = new TrainingSession();
        BeanUtils.copyProperties(form, session);
        session.setId(id);
        session.setTopic(form.getTopic().trim());
        session.setLocation(form.getLocation().trim());
        session.setStatus(blank(form.getStatus()) ? original.getStatus() : form.getStatus());
        session.setUpdateTime(LocalDateTime.now());
        if (trainingSessionMapper.updateById(session) == 0) {
            throw new ServiceRuntimeException("修改课次失败");
        }
        return Result.success("修改课次成功");
    }

    @Override
    @Transactional
    public Result<String> delete(Integer id) {
        TrainingSession session = trainingSessionMapper.selectById(id);
        if (session == null) {
            throw new ServiceRuntimeException("课次不存在");
        }
        requireEditableClass(session.getTrainingClassId());
        if (trainingSessionMapper.deleteById(id) == 0) {
            throw new ServiceRuntimeException("删除课次失败");
        }
        return Result.success("删除课次成功");
    }

    private void validate(TrainingSessionForm form, TrainingClass trainingClass, Integer excludeId) {
        TrainingValidationUtil.validateSessionDates(form, trainingClass);
        if (!blank(form.getStatus()) && !TrainingSessionStatus.supports(form.getStatus())) {
            throw new ServiceRuntimeException("课次状态不正确");
        }
        requireInstructor(form.getInstructorId());

        LambdaQueryWrapper<TrainingSession> numberWrapper = new LambdaQueryWrapper<TrainingSession>()
                .eq(TrainingSession::getTrainingClassId, form.getTrainingClassId())
                .eq(TrainingSession::getSessionNo, form.getSessionNo());
        if (excludeId != null) {
            numberWrapper.ne(TrainingSession::getId, excludeId);
        }
        if (trainingSessionMapper.selectCount(numberWrapper) > 0) {
            throw new ServiceRuntimeException("该班次的课次序号已存在");
        }

        QueryWrapper<TrainingSession> conflictWrapper = new QueryWrapper<>();
        conflictWrapper.eq("instructor_id", form.getInstructorId())
                .ne("status", TrainingSessionStatus.CANCELLED.name())
                .lt("start_time", form.getEndTime())
                .gt("end_time", form.getStartTime());
        if (excludeId != null) {
            conflictWrapper.ne("id", excludeId);
        }
        if (trainingSessionMapper.selectCount(conflictWrapper) > 0) {
            throw new ServiceRuntimeException("该讲师在所选时间已有其他课次");
        }
    }

    private TrainingClass requireClass(Integer id) {
        TrainingClass trainingClass = trainingClassMapper.selectById(id);
        if (trainingClass == null) {
            throw new ServiceRuntimeException("培训班次不存在");
        }
        return trainingClass;
    }

    private TrainingClass requireEditableClass(Integer id) {
        TrainingClass trainingClass = requireClass(id);
        if (!TrainingPublishStatus.DRAFT.name().equals(trainingClass.getPublishStatus())
                || TrainingClassStatus.CANCELLED.name().equals(trainingClass.getClassStatus())
                || !LocalDate.now().isBefore(trainingClass.getStartDate())) {
            throw new ServiceRuntimeException("只有尚未开课且未取消的草稿班次可以维护课次");
        }
        return trainingClass;
    }

    private User requireInstructor(Integer id) {
        User user = userMapper.selectById(id);
        if (user == null || !Integer.valueOf(2).equals(user.getRoleId()) || !Integer.valueOf(1).equals(user.getStatus())) {
            throw new ServiceRuntimeException("所选讲师不存在或已停用");
        }
        Long profileCount = instructorProfileMapper.selectCount(new LambdaQueryWrapper<InstructorProfile>()
                .eq(InstructorProfile::getUserId, id)
                .eq(InstructorProfile::getStatus, CourseStatus.ENABLED.name()));
        if (profileCount == null || profileCount == 0) {
            throw new ServiceRuntimeException("所选讲师尚未建立有效讲师档案");
        }
        return user;
    }

    private void ensureReadPermission(TrainingClass trainingClass) {
        Integer roleCode = SecurityUtil.getRoleCode();
        if (roleCode == 1) {
            ensurePubliclyVisible(trainingClass);
        }
        if (roleCode == 2 && !SecurityUtil.getUserId().equals(trainingClass.getInstructorId())) {
            Long ownSessions = trainingSessionMapper.selectCount(new LambdaQueryWrapper<TrainingSession>()
                    .eq(TrainingSession::getTrainingClassId, trainingClass.getId())
                    .eq(TrainingSession::getInstructorId, SecurityUtil.getUserId()));
            if (ownSessions == null || ownSessions == 0) {
                throw new ServiceRuntimeException("无权查看该培训班次的课次");
            }
        }
    }

    private void ensurePubliclyVisible(TrainingClass trainingClass) {
        Course course = courseMapper.selectById(trainingClass.getCourseId());
        if (!TrainingPublishStatus.PUBLISHED.name().equals(trainingClass.getPublishStatus())
                || TrainingClassStatus.CANCELLED.name().equals(trainingClass.getClassStatus())
                || course == null
                || !CourseStatus.ENABLED.name().equals(course.getStatus())) {
            throw new ServiceRuntimeException("培训班次不存在或尚未发布");
        }
    }

    private boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

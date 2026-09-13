package cn.org.alan.exam.service.impl;

import cn.org.alan.exam.common.exception.ServiceRuntimeException;
import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.mapper.CourseMapper;
import cn.org.alan.exam.mapper.InstructorProfileMapper;
import cn.org.alan.exam.mapper.TrainingClassMapper;
import cn.org.alan.exam.mapper.TrainingSessionMapper;
import cn.org.alan.exam.mapper.UserMapper;
import cn.org.alan.exam.model.entity.Course;
import cn.org.alan.exam.model.entity.InstructorProfile;
import cn.org.alan.exam.model.entity.TrainingClass;
import cn.org.alan.exam.model.entity.TrainingSession;
import cn.org.alan.exam.model.entity.User;
import cn.org.alan.exam.model.enums.CourseStatus;
import cn.org.alan.exam.model.enums.TrainingClassStatus;
import cn.org.alan.exam.model.enums.TrainingPublishStatus;
import cn.org.alan.exam.model.enums.TrainingSessionStatus;
import cn.org.alan.exam.model.form.training.TrainingClassForm;
import cn.org.alan.exam.model.vo.training.TrainingClassVO;
import cn.org.alan.exam.service.ITrainingClassService;
import cn.org.alan.exam.utils.SecurityUtil;
import cn.org.alan.exam.utils.TrainingValidationUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TrainingClassServiceImpl extends ServiceImpl<TrainingClassMapper, TrainingClass>
        implements ITrainingClassService {
    @Resource
    private TrainingClassMapper trainingClassMapper;
    @Resource
    private TrainingSessionMapper trainingSessionMapper;
    @Resource
    private CourseMapper courseMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private InstructorProfileMapper instructorProfileMapper;

    @Override
    public Result<IPage<TrainingClassVO>> paging(Integer pageNum, Integer pageSize, String keyword,
                                                 String classStatus, String publishStatus) {
        refreshClassStatuses();
        Integer instructorId = SecurityUtil.getRoleCode() == 2 ? SecurityUtil.getUserId() : null;
        Page<TrainingClassVO> page = trainingClassMapper.selectTrainingClassPage(
                new Page<>(pageNum, pageSize), keyword, classStatus, publishStatus, instructorId, false);
        return Result.success("查询成功", page);
    }

    @Override
    public Result<IPage<TrainingClassVO>> available(Integer pageNum, Integer pageSize, String keyword) {
        refreshClassStatuses();
        Page<TrainingClassVO> page = trainingClassMapper.selectTrainingClassPage(
                new Page<>(pageNum, pageSize), keyword, null, null, null, true);
        return Result.success("查询成功", page);
    }

    @Override
    public Result<TrainingClassVO> detail(Integer id) {
        refreshClassStatuses();
        TrainingClass trainingClass = requireClass(id);
        ensureReadPermission(trainingClass);
        TrainingClassVO result = trainingClassMapper.selectTrainingClassVOById(id);
        if (result == null) {
            throw new ServiceRuntimeException("培训班次不存在");
        }
        return Result.success("查询成功", result);
    }

    @Override
    public Result<TrainingClassVO> publicDetail(Integer id) {
        refreshClassStatuses();
        TrainingClass trainingClass = requireClass(id);
        ensurePubliclyVisible(trainingClass);
        TrainingClassVO result = trainingClassMapper.selectTrainingClassVOById(id);
        if (result == null) {
            throw new ServiceRuntimeException("培训班次不存在");
        }
        return Result.success("查询成功", result);
    }

    @Override
    @Transactional
    public Result<String> add(TrainingClassForm form) {
        TrainingValidationUtil.validateClassDates(form);
        requireFutureStartDate(form.getStartDate());
        requireValidEnrollmentPeriod(form.getEnrollmentStart(), form.getEnrollmentEnd());
        validateReferences(form.getCourseId(), form.getInstructorId());
        ensureClassCodeUnique(form.getClassCode(), null);
        TrainingClass trainingClass = new TrainingClass();
        copyForm(form, trainingClass);
        trainingClass.setClassStatus(TrainingClassStatus.PLANNED.name());
        trainingClass.setPublishStatus(TrainingPublishStatus.DRAFT.name());
        trainingClass.setVersion(0);
        trainingClass.setUpdateTime(LocalDateTime.now());
        if (trainingClassMapper.insert(trainingClass) == 0) {
            throw new ServiceRuntimeException("新增培训班次失败");
        }
        return Result.success("新增培训班次成功");
    }

    @Override
    @Transactional
    public Result<String> update(Integer id, TrainingClassForm form) {
        TrainingClass original = requireClass(id);
        requireDraft(original);
        if (TrainingClassStatus.CANCELLED.name().equals(original.getClassStatus())) {
            throw new ServiceRuntimeException("已取消的班次不能修改");
        }
        if (!LocalDate.now().isBefore(original.getStartDate())) {
            throw new ServiceRuntimeException("培训已经开始或结束，不能编辑班次");
        }
        TrainingValidationUtil.validateClassDates(form);
        requireFutureStartDate(form.getStartDate());
        requireValidEnrollmentPeriod(form.getEnrollmentStart(), form.getEnrollmentEnd());
        validateReferences(form.getCourseId(), form.getInstructorId());
        ensureClassCodeUnique(form.getClassCode(), id);
        copyForm(form, original);
        original.setUpdateTime(LocalDateTime.now());
        if (trainingClassMapper.updateById(original) == 0) {
            throw new ServiceRuntimeException("培训班次已被其他操作修改，请刷新后重试");
        }
        return Result.success("修改培训班次成功");
    }

    @Override
    @Transactional
    public Result<String> delete(Integer id) {
        TrainingClass trainingClass = requireClass(id);
        requireDraft(trainingClass);
        trainingSessionMapper.delete(new LambdaQueryWrapper<TrainingSession>()
                .eq(TrainingSession::getTrainingClassId, id));
        if (trainingClassMapper.deleteById(id) == 0) {
            throw new ServiceRuntimeException("删除培训班次失败");
        }
        return Result.success("删除培训班次成功");
    }

    @Override
    @Transactional
    public Result<String> publish(Integer id) {
        TrainingClass trainingClass = requireClass(id);
        if (!TrainingPublishStatus.DRAFT.name().equals(trainingClass.getPublishStatus())) {
            throw new ServiceRuntimeException("班次已经发布");
        }
        if (TrainingClassStatus.CANCELLED.name().equals(trainingClass.getClassStatus())) {
            throw new ServiceRuntimeException("已取消的班次不能发布");
        }
        if (!LocalDate.now().isBefore(trainingClass.getStartDate())) {
            throw new ServiceRuntimeException("培训已经开始或结束，不能发布班次");
        }
        requireValidEnrollmentPeriod(trainingClass.getEnrollmentStart(), trainingClass.getEnrollmentEnd());
        validateReferences(trainingClass.getCourseId(), trainingClass.getInstructorId());
        Long activeSessionCount = trainingSessionMapper.selectCount(new LambdaQueryWrapper<TrainingSession>()
                .eq(TrainingSession::getTrainingClassId, id)
                .ne(TrainingSession::getStatus, TrainingSessionStatus.CANCELLED.name()));
        if (activeSessionCount == null || activeSessionCount == 0) {
            throw new ServiceRuntimeException("至少安排一个未取消的课次后才能发布班次");
        }
        trainingClass.setPublishStatus(TrainingPublishStatus.PUBLISHED.name());
        trainingClass.setPublishTime(LocalDateTime.now());
        trainingClass.setClassStatus(TrainingValidationUtil.deriveClassStatus(trainingClass, LocalDateTime.now()));
        trainingClass.setUpdateTime(LocalDateTime.now());
        if (trainingClassMapper.updateById(trainingClass) == 0) {
            throw new ServiceRuntimeException("培训班次已被其他操作修改，请刷新后重试");
        }
        return Result.success("发布培训班次成功");
    }

    @Override
    @Transactional
    public Result<String> unpublish(Integer id) {
        TrainingClass trainingClass = requireClass(id);
        if (!TrainingPublishStatus.PUBLISHED.name().equals(trainingClass.getPublishStatus())) {
            throw new ServiceRuntimeException("班次尚未发布");
        }
        if (TrainingClassStatus.CANCELLED.name().equals(trainingClass.getClassStatus())) {
            throw new ServiceRuntimeException("已取消的班次不能撤回发布");
        }
        String currentStatus = TrainingValidationUtil.deriveClassStatus(trainingClass, LocalDateTime.now());
        if (!TrainingClassStatus.PLANNED.name().equals(currentStatus)
                && !TrainingClassStatus.ENROLLING.name().equals(currentStatus)) {
            throw new ServiceRuntimeException("只有未到报名或报名中的班次才能撤回");
        }
        trainingClass.setPublishStatus(TrainingPublishStatus.DRAFT.name());
        trainingClass.setClassStatus(TrainingClassStatus.PLANNED.name());
        trainingClass.setPublishTime(null);
        trainingClass.setUpdateTime(LocalDateTime.now());
        if (trainingClassMapper.updateById(trainingClass) == 0) {
            throw new ServiceRuntimeException("培训班次已被其他操作修改，请刷新后重试");
        }
        return Result.success("班次已撤回为草稿");
    }

    @Override
    @Transactional
    public Result<String> cancel(Integer id) {
        TrainingClass trainingClass = requireClass(id);
        if (TrainingClassStatus.CANCELLED.name().equals(trainingClass.getClassStatus())) {
            throw new ServiceRuntimeException("班次已经取消");
        }
        if (!TrainingPublishStatus.PUBLISHED.name().equals(trainingClass.getPublishStatus())) {
            throw new ServiceRuntimeException("未发布的草稿无需取消，请直接删除");
        }
        String currentStatus = TrainingValidationUtil.deriveClassStatus(trainingClass, LocalDateTime.now());
        if (TrainingClassStatus.IN_PROGRESS.name().equals(currentStatus)) {
            throw new ServiceRuntimeException("进行中的班次不能取消开班");
        }
        if (TrainingClassStatus.COMPLETED.name().equals(currentStatus)) {
            throw new ServiceRuntimeException("已结课班次不能取消");
        }
        trainingClass.setClassStatus(TrainingClassStatus.CANCELLED.name());
        trainingClass.setUpdateTime(LocalDateTime.now());
        if (trainingClassMapper.updateById(trainingClass) == 0) {
            throw new ServiceRuntimeException("培训班次已被其他操作修改，请刷新后重试");
        }
        return Result.success("培训班次已取消");
    }

    private void validateReferences(Integer courseId, Integer instructorId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null || !CourseStatus.ENABLED.name().equals(course.getStatus())) {
            throw new ServiceRuntimeException("所选课程不存在或已停用");
        }
        User instructor = userMapper.selectById(instructorId);
        if (instructor == null || !Integer.valueOf(2).equals(instructor.getRoleId())
                || !Integer.valueOf(1).equals(instructor.getStatus())) {
            throw new ServiceRuntimeException("所选讲师不存在或已停用");
        }
        Long profileCount = instructorProfileMapper.selectCount(new LambdaQueryWrapper<InstructorProfile>()
                .eq(InstructorProfile::getUserId, instructorId)
                .eq(InstructorProfile::getStatus, CourseStatus.ENABLED.name()));
        if (profileCount == null || profileCount == 0) {
            throw new ServiceRuntimeException("所选讲师尚未建立有效讲师档案");
        }
    }

    private void copyForm(TrainingClassForm form, TrainingClass target) {
        BeanUtils.copyProperties(form, target);
        target.setClassCode(form.getClassCode().trim());
        target.setClassName(form.getClassName().trim());
        target.setLocation(form.getLocation().trim());
    }

    private TrainingClass requireClass(Integer id) {
        TrainingClass trainingClass = trainingClassMapper.selectById(id);
        if (trainingClass == null) {
            throw new ServiceRuntimeException("培训班次不存在");
        }
        return trainingClass;
    }

    private void requireDraft(TrainingClass trainingClass) {
        if (!TrainingPublishStatus.DRAFT.name().equals(trainingClass.getPublishStatus())) {
            throw new ServiceRuntimeException("只有草稿班次可以修改或删除");
        }
    }

    private void ensureClassCodeUnique(String code, Integer excludeId) {
        LambdaQueryWrapper<TrainingClass> wrapper = new LambdaQueryWrapper<TrainingClass>()
                .eq(TrainingClass::getClassCode, code.trim());
        if (excludeId != null) {
            wrapper.ne(TrainingClass::getId, excludeId);
        }
        if (trainingClassMapper.selectCount(wrapper) > 0) {
            throw new ServiceRuntimeException("班次编码已存在");
        }
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
                throw new ServiceRuntimeException("无权查看该培训班次");
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

    private void refreshClassStatuses() {
        List<TrainingClass> classes = trainingClassMapper.selectList(new LambdaQueryWrapper<TrainingClass>()
                .ne(TrainingClass::getClassStatus, TrainingClassStatus.CANCELLED.name()));
        LocalDateTime now = LocalDateTime.now();
        for (TrainingClass trainingClass : classes) {
            String current = TrainingValidationUtil.deriveClassStatus(trainingClass, now);
            if (!current.equals(trainingClass.getClassStatus())) {
                trainingClass.setClassStatus(current);
                trainingClass.setUpdateTime(now);
                trainingClassMapper.updateById(trainingClass);
            }
        }
    }

    private void requireFutureStartDate(LocalDate startDate) {
        if (!LocalDate.now().isBefore(startDate)) {
            throw new ServiceRuntimeException("开课日期必须晚于当前日期");
        }
    }

    private void requireValidEnrollmentPeriod(LocalDateTime enrollmentStart, LocalDateTime enrollmentEnd) {
        LocalDateTime now = LocalDateTime.now();
        if (enrollmentStart.toLocalDate().isBefore(now.toLocalDate())) {
            throw new ServiceRuntimeException("报名开始日期不能早于当前日期");
        }
        if (!now.isBefore(enrollmentEnd)) {
            throw new ServiceRuntimeException("报名结束时间必须晚于当前时间");
        }
    }
}

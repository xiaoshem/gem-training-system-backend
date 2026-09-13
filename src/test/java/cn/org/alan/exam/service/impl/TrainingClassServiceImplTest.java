package cn.org.alan.exam.service.impl;

import cn.org.alan.exam.common.exception.ServiceRuntimeException;
import cn.org.alan.exam.mapper.CourseMapper;
import cn.org.alan.exam.mapper.InstructorProfileMapper;
import cn.org.alan.exam.mapper.TrainingClassMapper;
import cn.org.alan.exam.mapper.TrainingSessionMapper;
import cn.org.alan.exam.mapper.UserMapper;
import cn.org.alan.exam.model.entity.Course;
import cn.org.alan.exam.model.entity.TrainingClass;
import cn.org.alan.exam.model.entity.User;
import cn.org.alan.exam.model.enums.CourseStatus;
import cn.org.alan.exam.model.enums.TrainingClassStatus;
import cn.org.alan.exam.model.enums.TrainingPublishStatus;
import cn.org.alan.exam.model.form.training.TrainingClassForm;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TrainingClassServiceImplTest {

    @InjectMocks
    private TrainingClassServiceImpl service;
    @Mock
    private TrainingClassMapper trainingClassMapper;
    @Mock
    private TrainingSessionMapper trainingSessionMapper;
    @Mock
    private CourseMapper courseMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private InstructorProfileMapper instructorProfileMapper;

    @Test
    public void cancelRejectsDraftClass() {
        TrainingClass trainingClass = trainingClass(1, TrainingPublishStatus.DRAFT, TrainingClassStatus.PLANNED);
        when(trainingClassMapper.selectById(1)).thenReturn(trainingClass);

        try {
            service.cancel(1);
            fail("草稿班次不应该允许取消");
        } catch (ServiceRuntimeException exception) {
            assertEquals("未发布的草稿无需取消，请直接删除", exception.getMessage());
        }

        verify(trainingClassMapper, never()).updateById(any(TrainingClass.class));
    }

    @Test
    public void cancelAcceptsPublishedClass() {
        TrainingClass trainingClass = trainingClass(2, TrainingPublishStatus.PUBLISHED, TrainingClassStatus.ENROLLING);
        LocalDateTime now = LocalDateTime.now();
        trainingClass.setEnrollmentStart(now.minusDays(1));
        trainingClass.setEnrollmentEnd(now.plusDays(1));
        trainingClass.setStartDate(LocalDate.now().plusDays(2));
        trainingClass.setEndDate(LocalDate.now().plusDays(3));
        when(trainingClassMapper.selectById(2)).thenReturn(trainingClass);
        when(trainingClassMapper.updateById(any(TrainingClass.class))).thenReturn(1);

        service.cancel(2);

        ArgumentCaptor<TrainingClass> captor = ArgumentCaptor.forClass(TrainingClass.class);
        verify(trainingClassMapper).updateById(captor.capture());
        assertEquals(TrainingClassStatus.CANCELLED.name(), captor.getValue().getClassStatus());
        assertEquals(TrainingPublishStatus.PUBLISHED.name(), captor.getValue().getPublishStatus());
    }

    @Test
    public void cancelRejectsInProgressClass() {
        TrainingClass trainingClass = trainingClass(11, TrainingPublishStatus.PUBLISHED, TrainingClassStatus.IN_PROGRESS);
        LocalDateTime now = LocalDateTime.now();
        trainingClass.setEnrollmentStart(now.minusDays(3));
        trainingClass.setEnrollmentEnd(now.minusDays(2));
        trainingClass.setStartDate(LocalDate.now());
        trainingClass.setEndDate(LocalDate.now().plusDays(1));
        when(trainingClassMapper.selectById(11)).thenReturn(trainingClass);

        try {
            service.cancel(11);
            fail("进行中的班次不应该允许取消开班");
        } catch (ServiceRuntimeException exception) {
            assertEquals("进行中的班次不能取消开班", exception.getMessage());
        }

        verify(trainingClassMapper, never()).updateById(any(TrainingClass.class));
    }

    @Test
    public void unpublishRejectsUpcomingClass() {
        TrainingClass trainingClass = publishedClass(3, TrainingClassStatus.UPCOMING);
        LocalDateTime now = LocalDateTime.now();
        trainingClass.setEnrollmentStart(now.minusDays(2));
        trainingClass.setEnrollmentEnd(now.minusDays(1));
        trainingClass.setStartDate(LocalDate.now().plusDays(1));
        trainingClass.setEndDate(LocalDate.now().plusDays(2));
        when(trainingClassMapper.selectById(3)).thenReturn(trainingClass);

        try {
            service.unpublish(3);
            fail("待开课班次不应该允许撤回");
        } catch (ServiceRuntimeException exception) {
            assertEquals("只有未到报名或报名中的班次才能撤回", exception.getMessage());
        }

        verify(trainingClassMapper, never()).updateById(any(TrainingClass.class));
    }

    @Test
    public void unpublishAcceptsPlannedClass() {
        TrainingClass trainingClass = publishedClass(4, TrainingClassStatus.PLANNED);
        LocalDateTime now = LocalDateTime.now();
        trainingClass.setEnrollmentStart(now.plusDays(1));
        trainingClass.setEnrollmentEnd(now.plusDays(2));
        trainingClass.setStartDate(LocalDate.now().plusDays(3));
        trainingClass.setEndDate(LocalDate.now().plusDays(4));
        when(trainingClassMapper.selectById(4)).thenReturn(trainingClass);
        when(trainingClassMapper.updateById(any(TrainingClass.class))).thenReturn(1);

        service.unpublish(4);

        assertEquals(TrainingPublishStatus.DRAFT.name(), trainingClass.getPublishStatus());
        assertEquals(TrainingClassStatus.PLANNED.name(), trainingClass.getClassStatus());
        verify(trainingClassMapper).updateById(trainingClass);
    }

    @Test
    public void unpublishAcceptsEnrollingClass() {
        TrainingClass trainingClass = publishedClass(5, TrainingClassStatus.ENROLLING);
        LocalDateTime now = LocalDateTime.now();
        trainingClass.setEnrollmentStart(now.minusDays(1));
        trainingClass.setEnrollmentEnd(now.plusDays(1));
        trainingClass.setStartDate(LocalDate.now().plusDays(2));
        trainingClass.setEndDate(LocalDate.now().plusDays(3));
        when(trainingClassMapper.selectById(5)).thenReturn(trainingClass);
        when(trainingClassMapper.updateById(any(TrainingClass.class))).thenReturn(1);

        service.unpublish(5);

        assertEquals(TrainingPublishStatus.DRAFT.name(), trainingClass.getPublishStatus());
        assertEquals(TrainingClassStatus.PLANNED.name(), trainingClass.getClassStatus());
        verify(trainingClassMapper).updateById(trainingClass);
    }

    @Test
    public void addRejectsClassThatHasAlreadyStarted() {
        LocalDateTime now = LocalDateTime.now();
        TrainingClassForm form = new TrainingClassForm();
        form.setEnrollmentStart(now.minusDays(4));
        form.setEnrollmentEnd(now.minusDays(3));
        form.setStartDate(LocalDate.now().minusDays(2));
        form.setEndDate(LocalDate.now().minusDays(1));

        try {
            service.add(form);
            fail("不应该允许创建已经开始或结束的班次");
        } catch (ServiceRuntimeException exception) {
            assertEquals("开课日期必须晚于当前日期", exception.getMessage());
        }

        verify(trainingClassMapper, never()).insert(any(TrainingClass.class));
    }

    @Test
    public void addRejectsPastEnrollmentDates() {
        LocalDateTime now = LocalDateTime.now();
        TrainingClassForm form = new TrainingClassForm();
        form.setEnrollmentStart(now.minusDays(3));
        form.setEnrollmentEnd(now.minusDays(2));
        form.setStartDate(LocalDate.now().plusDays(1));
        form.setEndDate(LocalDate.now().plusDays(2));

        try {
            service.add(form);
            fail("不应该允许保存报名日期已经过去的班次");
        } catch (ServiceRuntimeException exception) {
            assertEquals("报名开始日期不能早于当前日期", exception.getMessage());
        }

        verify(trainingClassMapper, never()).insert(any(TrainingClass.class));
    }

    @Test
    public void updateRejectsExpiredDraftClass() {
        TrainingClass trainingClass = trainingClass(6, TrainingPublishStatus.DRAFT, TrainingClassStatus.PLANNED);
        trainingClass.setStartDate(LocalDate.now().minusDays(2));
        trainingClass.setEndDate(LocalDate.now().minusDays(1));
        when(trainingClassMapper.selectById(6)).thenReturn(trainingClass);

        try {
            service.update(6, new TrainingClassForm());
            fail("过期草稿不应该允许编辑");
        } catch (ServiceRuntimeException exception) {
            assertEquals("培训已经开始或结束，不能编辑班次", exception.getMessage());
        }

        verify(trainingClassMapper, never()).updateById(any(TrainingClass.class));
    }

    @Test
    public void publishRejectsExpiredDraftClass() {
        TrainingClass trainingClass = trainingClass(7, TrainingPublishStatus.DRAFT, TrainingClassStatus.PLANNED);
        trainingClass.setStartDate(LocalDate.now().minusDays(2));
        trainingClass.setEndDate(LocalDate.now().minusDays(1));
        when(trainingClassMapper.selectById(7)).thenReturn(trainingClass);

        try {
            service.publish(7);
            fail("过期草稿不应该允许发布");
        } catch (ServiceRuntimeException exception) {
            assertEquals("培训已经开始或结束，不能发布班次", exception.getMessage());
        }

        verify(trainingClassMapper, never()).updateById(any(TrainingClass.class));
    }

    @Test
    public void publishRejectsExpiredEnrollmentPeriod() {
        TrainingClass trainingClass = trainingClass(10, TrainingPublishStatus.DRAFT, TrainingClassStatus.PLANNED);
        trainingClass.setEnrollmentStart(LocalDate.now().atStartOfDay());
        trainingClass.setEnrollmentEnd(LocalDateTime.now().minusSeconds(1));
        trainingClass.setStartDate(LocalDate.now().plusDays(1));
        trainingClass.setEndDate(LocalDate.now().plusDays(2));
        when(trainingClassMapper.selectById(10)).thenReturn(trainingClass);

        try {
            service.publish(10);
            fail("报名时间已结束的草稿不应该允许发布");
        } catch (ServiceRuntimeException exception) {
            assertEquals("报名结束时间必须晚于当前时间", exception.getMessage());
        }

        verify(trainingClassMapper, never()).updateById(any(TrainingClass.class));
    }

    @Test
    public void publishRejectsClassWhoseSessionsAreAllCancelled() {
        TrainingClass trainingClass = publishableDraftClass(12);
        prepareValidPublishReferences(trainingClass);
        when(trainingSessionMapper.selectCount(any())).thenReturn(0L);

        try {
            service.publish(12);
            fail("全部课次已取消的班次不应该允许发布");
        } catch (ServiceRuntimeException exception) {
            assertEquals("至少安排一个未取消的课次后才能发布班次", exception.getMessage());
        }

        verify(trainingClassMapper, never()).updateById(any(TrainingClass.class));
    }

    @Test
    public void publishAcceptsClassWithPlannedSession() {
        TrainingClass trainingClass = publishableDraftClass(13);
        prepareValidPublishReferences(trainingClass);
        when(trainingSessionMapper.selectCount(any())).thenReturn(1L);
        when(trainingClassMapper.updateById(any(TrainingClass.class))).thenReturn(1);

        service.publish(13);

        assertEquals(TrainingPublishStatus.PUBLISHED.name(), trainingClass.getPublishStatus());
        verify(trainingClassMapper).updateById(trainingClass);
    }

    @Test
    public void statusRefreshMarksExpiredDraftAsExpired() {
        TrainingClass trainingClass = trainingClass(8, TrainingPublishStatus.DRAFT, TrainingClassStatus.PLANNED);
        LocalDateTime now = LocalDateTime.now();
        trainingClass.setEnrollmentStart(now.minusDays(4));
        trainingClass.setEnrollmentEnd(now.minusDays(3));
        trainingClass.setStartDate(LocalDate.now().minusDays(2));
        trainingClass.setEndDate(LocalDate.now().minusDays(1));
        when(trainingClassMapper.selectList(any())).thenReturn(Collections.singletonList(trainingClass));
        when(trainingClassMapper.updateById(any(TrainingClass.class))).thenReturn(1);

        ReflectionTestUtils.invokeMethod(service, "refreshClassStatuses");

        assertEquals(TrainingClassStatus.EXPIRED.name(), trainingClass.getClassStatus());
        verify(trainingClassMapper).updateById(trainingClass);
    }

    @Test
    public void statusRefreshKeepsFutureDraftInactiveDuringEnrollmentDates() {
        TrainingClass trainingClass = trainingClass(9, TrainingPublishStatus.DRAFT, TrainingClassStatus.ENROLLING);
        LocalDateTime now = LocalDateTime.now();
        trainingClass.setEnrollmentStart(now.minusDays(1));
        trainingClass.setEnrollmentEnd(now.plusDays(1));
        trainingClass.setStartDate(LocalDate.now().plusDays(2));
        trainingClass.setEndDate(LocalDate.now().plusDays(3));
        when(trainingClassMapper.selectList(any())).thenReturn(Collections.singletonList(trainingClass));
        when(trainingClassMapper.updateById(any(TrainingClass.class))).thenReturn(1);

        ReflectionTestUtils.invokeMethod(service, "refreshClassStatuses");

        assertEquals(TrainingClassStatus.PLANNED.name(), trainingClass.getClassStatus());
        verify(trainingClassMapper).updateById(trainingClass);
    }

    private TrainingClass trainingClass(Integer id, TrainingPublishStatus publishStatus,
                                        TrainingClassStatus classStatus) {
        TrainingClass trainingClass = new TrainingClass();
        trainingClass.setId(id);
        trainingClass.setPublishStatus(publishStatus.name());
        trainingClass.setClassStatus(classStatus.name());
        return trainingClass;
    }

    private TrainingClass publishedClass(Integer id, TrainingClassStatus classStatus) {
        return trainingClass(id, TrainingPublishStatus.PUBLISHED, classStatus);
    }

    private TrainingClass publishableDraftClass(Integer id) {
        LocalDateTime now = LocalDateTime.now();
        TrainingClass trainingClass = trainingClass(id, TrainingPublishStatus.DRAFT, TrainingClassStatus.PLANNED);
        trainingClass.setCourseId(1);
        trainingClass.setInstructorId(2);
        trainingClass.setEnrollmentStart(now.plusHours(1));
        trainingClass.setEnrollmentEnd(now.plusHours(2));
        trainingClass.setStartDate(LocalDate.now().plusDays(1));
        trainingClass.setEndDate(LocalDate.now().plusDays(2));
        return trainingClass;
    }

    private void prepareValidPublishReferences(TrainingClass trainingClass) {
        Course course = new Course();
        course.setStatus(CourseStatus.ENABLED.name());
        User instructor = new User();
        instructor.setRoleId(2);
        instructor.setStatus(1);
        when(trainingClassMapper.selectById(trainingClass.getId())).thenReturn(trainingClass);
        when(courseMapper.selectById(trainingClass.getCourseId())).thenReturn(course);
        when(userMapper.selectById(trainingClass.getInstructorId())).thenReturn(instructor);
        when(instructorProfileMapper.selectCount(any())).thenReturn(1L);
    }
}

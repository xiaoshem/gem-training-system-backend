package cn.org.alan.exam.service.impl;

import cn.org.alan.exam.common.exception.ServiceRuntimeException;
import cn.org.alan.exam.mapper.EnrollmentAuditLogMapper;
import cn.org.alan.exam.mapper.PaymentOrderMapper;
import cn.org.alan.exam.mapper.TrainingClassMapper;
import cn.org.alan.exam.mapper.TrainingEnrollmentMapper;
import cn.org.alan.exam.mapper.UserMessageMapper;
import cn.org.alan.exam.model.entity.EnrollmentAuditLog;
import cn.org.alan.exam.model.entity.PaymentOrder;
import cn.org.alan.exam.model.entity.TrainingClass;
import cn.org.alan.exam.model.entity.TrainingEnrollment;
import cn.org.alan.exam.model.entity.User;
import cn.org.alan.exam.model.entity.UserMessage;
import cn.org.alan.exam.model.enums.EnrollmentStatus;
import cn.org.alan.exam.model.enums.TrainingClassStatus;
import cn.org.alan.exam.model.enums.TrainingPublishStatus;
import cn.org.alan.exam.model.form.enrollment.EnrollmentApplicationForm;
import cn.org.alan.exam.model.form.enrollment.EnrollmentReviewForm;
import cn.org.alan.exam.model.vo.enrollment.EnrollmentVO;
import cn.org.alan.exam.utils.security.SysUserDetails;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EnrollmentServiceImplTest {
    @InjectMocks
    private EnrollmentServiceImpl service;
    @Mock
    private TrainingEnrollmentMapper enrollmentMapper;
    @Mock
    private TrainingClassMapper trainingClassMapper;
    @Mock
    private EnrollmentAuditLogMapper auditLogMapper;
    @Mock
    private PaymentOrderMapper paymentOrderMapper;
    @Mock
    private UserMessageMapper userMessageMapper;

    @Before
    public void setUp() {
        authenticate(101, "role_student");
    }

    @After
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void applyCreatesSnapshotAndAuditLog() {
        TrainingClass trainingClass = enrollingClass(1, 2);
        when(trainingClassMapper.selectById(1)).thenReturn(trainingClass);
        when(enrollmentMapper.countAdmitted(1)).thenReturn(0);
        when(enrollmentMapper.insert(any(TrainingEnrollment.class))).thenAnswer(invocation -> {
            TrainingEnrollment value = invocation.getArgument(0);
            value.setId(9);
            return 1;
        });
        when(auditLogMapper.insert(any(EnrollmentAuditLog.class))).thenReturn(1);

        service.apply(applicationForm(1));

        ArgumentCaptor<TrainingEnrollment> enrollmentCaptor = ArgumentCaptor.forClass(TrainingEnrollment.class);
        verify(enrollmentMapper).insert(enrollmentCaptor.capture());
        assertEquals(Integer.valueOf(101), enrollmentCaptor.getValue().getStudentId());
        assertEquals("测试学员", enrollmentCaptor.getValue().getSnapshotRealName());
        assertEquals(EnrollmentStatus.PENDING.name(), enrollmentCaptor.getValue().getStatus());
        assertNotNull(enrollmentCaptor.getValue().getEnrollmentNo());
        verify(auditLogMapper).insert(any(EnrollmentAuditLog.class));
    }

    @Test
    public void applyRejectsOutsideEnrollmentWindow() {
        TrainingClass trainingClass = enrollingClass(2, 2);
        trainingClass.setEnrollmentStart(LocalDateTime.now().plusDays(1));
        trainingClass.setEnrollmentEnd(LocalDateTime.now().plusDays(2));
        when(trainingClassMapper.selectById(2)).thenReturn(trainingClass);

        try {
            service.apply(applicationForm(2));
            fail("未到报名时间不应该允许报名");
        } catch (ServiceRuntimeException exception) {
            assertEquals("当前不在该班次的报名时间内", exception.getMessage());
        }
        verify(enrollmentMapper, never()).insert(any(TrainingEnrollment.class));
    }

    @Test
    public void admitRejectsWhenCapacityIsFull() {
        authenticate(3, "role_admin");
        TrainingEnrollment enrollment = pendingEnrollment(8, 1, 101);
        TrainingClass trainingClass = enrollingClass(1, 1);
        when(enrollmentMapper.selectById(8)).thenReturn(enrollment);
        when(trainingClassMapper.selectByIdForUpdate(1)).thenReturn(trainingClass);
        when(enrollmentMapper.selectByIdForUpdate(8)).thenReturn(enrollment);
        when(enrollmentMapper.countAdmitted(1)).thenReturn(1);

        try {
            service.admit(8, new EnrollmentReviewForm());
            fail("名额已满不应该继续录取");
        } catch (ServiceRuntimeException exception) {
            assertEquals("招生名额已满，不能继续录取", exception.getMessage());
        }
        verify(paymentOrderMapper, never()).insert(any(PaymentOrder.class));
    }

    @Test
    public void admitCreatesOrderAuditAndMessage() {
        authenticate(3, "role_admin");
        TrainingEnrollment enrollment = pendingEnrollment(10, 1, 101);
        TrainingClass trainingClass = enrollingClass(1, 2);
        when(enrollmentMapper.selectById(10)).thenReturn(enrollment);
        when(trainingClassMapper.selectByIdForUpdate(1)).thenReturn(trainingClass);
        when(enrollmentMapper.selectByIdForUpdate(10)).thenReturn(enrollment);
        when(enrollmentMapper.countAdmitted(1)).thenReturn(0);
        when(enrollmentMapper.updateById(enrollment)).thenReturn(1);
        when(paymentOrderMapper.insert(any(PaymentOrder.class))).thenReturn(1);
        when(auditLogMapper.insert(any(EnrollmentAuditLog.class))).thenReturn(1);
        when(userMessageMapper.insert(any(UserMessage.class))).thenReturn(1);

        EnrollmentReviewForm form = new EnrollmentReviewForm();
        form.setReason("资料齐全");
        service.admit(10, form);

        assertEquals(EnrollmentStatus.ADMITTED.name(), enrollment.getStatus());
        ArgumentCaptor<PaymentOrder> orderCaptor = ArgumentCaptor.forClass(PaymentOrder.class);
        verify(paymentOrderMapper).insert(orderCaptor.capture());
        assertEquals(new BigDecimal("680.00"), orderCaptor.getValue().getAmount());
        verify(auditLogMapper).insert(any(EnrollmentAuditLog.class));
        verify(userMessageMapper).insert(any(UserMessage.class));
    }

    @Test
    public void rejectRequiresReason() {
        authenticate(3, "role_admin");
        EnrollmentReviewForm form = new EnrollmentReviewForm();
        try {
            service.reject(10, form);
            fail("拒绝报名必须填写原因");
        } catch (ServiceRuntimeException exception) {
            assertEquals("拒绝报名时必须填写原因", exception.getMessage());
        }
        verify(enrollmentMapper, never()).updateById(any(TrainingEnrollment.class));
    }

    @Test
    public void manageShowsFullPhoneButKeepsIdCardMasked() {
        authenticate(3, "role_admin");
        EnrollmentVO enrollment = new EnrollmentVO();
        enrollment.setPhone("13312349331");
        enrollment.setIdCard("450102200001010011");
        Page<EnrollmentVO> page = new Page<>(1, 10);
        page.setRecords(Collections.singletonList(enrollment));
        when(enrollmentMapper.selectEnrollmentPage(any(Page.class), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(page);

        service.manage(1, 10, null, null, null);

        assertEquals("13312349331", page.getRecords().get(0).getPhone());
        assertEquals("450102********0011", page.getRecords().get(0).getIdCard());
    }

    private TrainingClass enrollingClass(Integer id, Integer capacity) {
        TrainingClass trainingClass = new TrainingClass();
        trainingClass.setId(id);
        trainingClass.setClassName("人工宝石技能培训班");
        trainingClass.setPublishStatus(TrainingPublishStatus.PUBLISHED.name());
        trainingClass.setClassStatus(TrainingClassStatus.ENROLLING.name());
        trainingClass.setEnrollmentStart(LocalDateTime.now().minusHours(1));
        trainingClass.setEnrollmentEnd(LocalDateTime.now().plusHours(2));
        trainingClass.setStartDate(LocalDate.now().plusDays(2));
        trainingClass.setEndDate(LocalDate.now().plusDays(3));
        trainingClass.setCapacity(capacity);
        trainingClass.setFee(new BigDecimal("680.00"));
        return trainingClass;
    }

    private EnrollmentApplicationForm applicationForm(Integer classId) {
        EnrollmentApplicationForm form = new EnrollmentApplicationForm();
        form.setTrainingClassId(classId);
        form.setRealName("测试学员");
        form.setIdCard("450102200001010011");
        form.setOrganization("测试单位");
        form.setPosition("操作员");
        form.setPhone("13800138000");
        return form;
    }

    private TrainingEnrollment pendingEnrollment(Integer id, Integer classId, Integer studentId) {
        TrainingEnrollment enrollment = new TrainingEnrollment();
        enrollment.setId(id);
        enrollment.setTrainingClassId(classId);
        enrollment.setStudentId(studentId);
        enrollment.setStatus(EnrollmentStatus.PENDING.name());
        enrollment.setVersion(0);
        return enrollment;
    }

    private void authenticate(Integer userId, String role) {
        User user = new User();
        user.setId(userId);
        user.setUserName("test" + userId);
        SysUserDetails principal = new SysUserDetails(user);
        principal.setPermissions(Collections.singletonList(new SimpleGrantedAuthority(role)));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
}

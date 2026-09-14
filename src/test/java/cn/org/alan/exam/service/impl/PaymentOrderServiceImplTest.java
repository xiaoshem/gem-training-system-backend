package cn.org.alan.exam.service.impl;

import cn.org.alan.exam.common.exception.ServiceRuntimeException;
import cn.org.alan.exam.mapper.PaymentOrderMapper;
import cn.org.alan.exam.mapper.PaymentRecordMapper;
import cn.org.alan.exam.mapper.TrainingClassMapper;
import cn.org.alan.exam.mapper.TrainingEnrollmentMapper;
import cn.org.alan.exam.mapper.UserMessageMapper;
import cn.org.alan.exam.model.entity.PaymentOrder;
import cn.org.alan.exam.model.entity.PaymentRecord;
import cn.org.alan.exam.model.entity.TrainingClass;
import cn.org.alan.exam.model.entity.TrainingEnrollment;
import cn.org.alan.exam.model.entity.User;
import cn.org.alan.exam.model.entity.UserMessage;
import cn.org.alan.exam.model.enums.EnrollmentStatus;
import cn.org.alan.exam.model.enums.PaymentOrderStatus;
import cn.org.alan.exam.model.enums.TrainingClassStatus;
import cn.org.alan.exam.utils.security.SysUserDetails;
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
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PaymentOrderServiceImplTest {
    @InjectMocks
    private PaymentOrderServiceImpl service;
    @Mock
    private PaymentOrderMapper paymentOrderMapper;
    @Mock
    private PaymentRecordMapper paymentRecordMapper;
    @Mock
    private TrainingEnrollmentMapper enrollmentMapper;
    @Mock
    private TrainingClassMapper trainingClassMapper;
    @Mock
    private UserMessageMapper userMessageMapper;

    @Before
    public void setUp() {
        User user = new User();
        user.setId(101);
        user.setUserName("student101");
        SysUserDetails principal = new SysUserDetails(user);
        principal.setPermissions(Collections.singletonList(new SimpleGrantedAuthority("role_student")));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @After
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void payRejectsDuplicatePayment() {
        PaymentOrder order = order(PaymentOrderStatus.PAID);
        when(paymentOrderMapper.selectById(5)).thenReturn(order);
        try {
            service.pay(5);
            fail("已支付订单不应该重复支付");
        } catch (ServiceRuntimeException exception) {
            assertEquals("该订单已经支付，请勿重复缴费", exception.getMessage());
        }
    }

    @Test
    public void payCreatesRecordAndMessage() {
        PaymentOrder order = order(PaymentOrderStatus.UNPAID);
        TrainingEnrollment enrollment = new TrainingEnrollment();
        enrollment.setId(9);
        enrollment.setStatus(EnrollmentStatus.ADMITTED.name());
        TrainingClass trainingClass = new TrainingClass();
        trainingClass.setId(1);
        trainingClass.setClassName("人工宝石技能培训班");
        trainingClass.setClassStatus(TrainingClassStatus.UPCOMING.name());
        trainingClass.setStartDate(LocalDate.now().plusDays(2));
        when(paymentOrderMapper.selectById(5)).thenReturn(order);
        when(paymentOrderMapper.selectByIdForUpdate(5)).thenReturn(order);
        when(enrollmentMapper.selectByIdForUpdate(9)).thenReturn(enrollment);
        when(trainingClassMapper.selectById(1)).thenReturn(trainingClass);
        when(paymentOrderMapper.updateById(order)).thenReturn(1);
        when(paymentRecordMapper.insert(any(PaymentRecord.class))).thenReturn(1);
        when(userMessageMapper.insert(any(UserMessage.class))).thenReturn(1);

        service.pay(5);

        assertEquals(PaymentOrderStatus.PAID.name(), order.getStatus());
        assertNotNull(order.getTransactionNo());
        ArgumentCaptor<PaymentRecord> recordCaptor = ArgumentCaptor.forClass(PaymentRecord.class);
        verify(paymentRecordMapper).insert(recordCaptor.capture());
        assertEquals(order.getAmount(), recordCaptor.getValue().getAmount());
        verify(userMessageMapper).insert(any(UserMessage.class));
    }

    private PaymentOrder order(PaymentOrderStatus status) {
        PaymentOrder order = new PaymentOrder();
        order.setId(5);
        order.setOrderNo("DD202609130001");
        order.setEnrollmentId(9);
        order.setStudentId(101);
        order.setTrainingClassId(1);
        order.setAmount(new BigDecimal("680.00"));
        order.setStatus(status.name());
        order.setVersion(0);
        return order;
    }
}

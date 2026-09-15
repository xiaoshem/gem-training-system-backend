package cn.org.alan.exam.service.impl;

import cn.org.alan.exam.common.exception.ServiceRuntimeException;
import cn.org.alan.exam.config.PaymentProperties;
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
import cn.org.alan.exam.model.dto.payment.AlipayNotification;
import cn.org.alan.exam.model.enums.EnrollmentStatus;
import cn.org.alan.exam.model.enums.PaymentOrderStatus;
import cn.org.alan.exam.model.enums.TrainingClassStatus;
import cn.org.alan.exam.model.vo.payment.PaymentOrderVO;
import cn.org.alan.exam.utils.security.SysUserDetails;
import cn.org.alan.exam.service.payment.AlipaySandboxGateway;
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
import java.util.Collections;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
    @Mock
    private PaymentProperties paymentProperties;
    @Mock
    private AlipaySandboxGateway alipaySandboxGateway;

    @Before
    public void setUp() {
        when(paymentProperties.isLocalSimulationEnabled()).thenReturn(true);
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
    public void mineShowsFullTransactionNumberToOrderOwner() {
        String transactionNo = "202609142200000000001638";
        PaymentOrderVO order = new PaymentOrderVO();
        order.setTransactionNo(transactionNo);
        Page<PaymentOrderVO> page = new Page<>(1, 10);
        page.setRecords(Collections.singletonList(order));
        when(paymentOrderMapper.selectPaymentOrderPage(any(Page.class), eq(101), isNull(), isNull()))
                .thenReturn(page);

        service.mine(1, 10, null, null);

        assertEquals(transactionNo, page.getRecords().get(0).getTransactionNo());
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
        assertEquals("SIMULATED", recordCaptor.getValue().getPaymentMethod());
        assertEquals("SIMULATED", order.getPaymentChannel());
        verify(userMessageMapper).insert(any(UserMessage.class));
    }

    @Test
    public void alipayNotificationCompletesOrder() {
        PaymentOrder order = order(PaymentOrderStatus.UNPAID);
        TrainingClass trainingClass = new TrainingClass();
        trainingClass.setClassName("人工宝石技能培训班");
        AlipayNotification notification = new AlipayNotification();
        notification.setOrderNo(order.getOrderNo());
        notification.setTransactionNo("202609142200000000000001");
        notification.setTradeStatus("TRADE_SUCCESS");
        notification.setTotalAmount(order.getAmount());
        when(alipaySandboxGateway.verifyNotification(any())).thenReturn(notification);
        when(paymentOrderMapper.selectByOrderNoForUpdate(order.getOrderNo())).thenReturn(order);
        when(paymentOrderMapper.updateById(order)).thenReturn(1);
        when(paymentRecordMapper.insert(any(PaymentRecord.class))).thenReturn(1);
        when(trainingClassMapper.selectById(1)).thenReturn(trainingClass);
        when(userMessageMapper.insert(any(UserMessage.class))).thenReturn(1);

        service.handleAlipayNotification(new HashMap<>());

        assertEquals(PaymentOrderStatus.PAID.name(), order.getStatus());
        assertEquals("ALIPAY_SANDBOX", order.getPaymentChannel());
        assertEquals(notification.getTransactionNo(), order.getTransactionNo());
    }

    @Test
    public void alipayNotificationRejectsAmountMismatch() {
        PaymentOrder order = order(PaymentOrderStatus.UNPAID);
        AlipayNotification notification = new AlipayNotification();
        notification.setOrderNo(order.getOrderNo());
        notification.setTransactionNo("202609142200000000000002");
        notification.setTradeStatus("TRADE_SUCCESS");
        notification.setTotalAmount(new BigDecimal("1.00"));
        when(alipaySandboxGateway.verifyNotification(any())).thenReturn(notification);
        when(paymentOrderMapper.selectByOrderNoForUpdate(order.getOrderNo())).thenReturn(order);

        try {
            service.handleAlipayNotification(new HashMap<>());
            fail("金额不一致的支付宝通知不应该入账");
        } catch (ServiceRuntimeException exception) {
            assertEquals("支付金额与订单金额不一致", exception.getMessage());
        }
    }

    @Test
    public void alipaySignedReturnCompletesOrderWithoutDependingOnActiveQuery() {
        PaymentOrder order = order(PaymentOrderStatus.UNPAID);
        TrainingClass trainingClass = new TrainingClass();
        trainingClass.setClassName("人工宝石技能培训班");
        PaymentProperties.AlipaySandbox sandbox = new PaymentProperties.AlipaySandbox();
        sandbox.setFrontendReturnUrl("http://localhost:9527/#/my-payments");
        AlipayNotification returnResult = new AlipayNotification();
        returnResult.setOrderNo(order.getOrderNo());
        returnResult.setTransactionNo("202609142200000000000003");
        returnResult.setTradeStatus("TRADE_SUCCESS");
        returnResult.setTotalAmount(order.getAmount());
        when(paymentProperties.getAlipaySandbox()).thenReturn(sandbox);
        when(alipaySandboxGateway.verifyReturn(any())).thenReturn(returnResult);
        when(paymentOrderMapper.selectByOrderNoForUpdate(order.getOrderNo())).thenReturn(order);
        when(paymentOrderMapper.updateById(order)).thenReturn(1);
        when(paymentRecordMapper.insert(any(PaymentRecord.class))).thenReturn(1);
        when(trainingClassMapper.selectById(1)).thenReturn(trainingClass);
        when(userMessageMapper.insert(any(UserMessage.class))).thenReturn(1);

        String redirect = service.handleAlipayReturn(new HashMap<>());

        assertEquals(PaymentOrderStatus.PAID.name(), order.getStatus());
        assertEquals("ALIPAY_SANDBOX", order.getPaymentChannel());
        assertEquals(returnResult.getTransactionNo(), order.getTransactionNo());
        assertTrue(redirect.contains("alipayReturn=paid"));
        assertTrue(redirect.contains("outTradeNo=" + order.getOrderNo()));
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

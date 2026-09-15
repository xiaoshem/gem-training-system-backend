package cn.org.alan.exam.service.impl;

import cn.org.alan.exam.common.exception.ServiceRuntimeException;
import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.config.PaymentProperties;
import cn.org.alan.exam.mapper.PaymentOrderMapper;
import cn.org.alan.exam.mapper.PaymentRecordMapper;
import cn.org.alan.exam.mapper.TrainingClassMapper;
import cn.org.alan.exam.mapper.TrainingEnrollmentMapper;
import cn.org.alan.exam.mapper.UserMessageMapper;
import cn.org.alan.exam.model.dto.payment.AlipayNotification;
import cn.org.alan.exam.model.dto.payment.AlipayTradeQueryResult;
import cn.org.alan.exam.model.entity.PaymentOrder;
import cn.org.alan.exam.model.entity.PaymentRecord;
import cn.org.alan.exam.model.entity.TrainingClass;
import cn.org.alan.exam.model.entity.TrainingEnrollment;
import cn.org.alan.exam.model.entity.UserMessage;
import cn.org.alan.exam.model.enums.EnrollmentStatus;
import cn.org.alan.exam.model.enums.PaymentOrderStatus;
import cn.org.alan.exam.model.enums.TrainingClassStatus;
import cn.org.alan.exam.model.vo.payment.AlipayPagePayVO;
import cn.org.alan.exam.model.vo.payment.AlipayPaymentQueryVO;
import cn.org.alan.exam.model.vo.payment.PaymentCapabilitiesVO;
import cn.org.alan.exam.model.vo.payment.PaymentOrderVO;
import cn.org.alan.exam.service.IPaymentOrderService;
import cn.org.alan.exam.service.payment.AlipaySandboxGateway;
import cn.org.alan.exam.utils.SecurityUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentOrderServiceImpl extends ServiceImpl<PaymentOrderMapper, PaymentOrder>
        implements IPaymentOrderService {
    private static final String CHANNEL_SIMULATED = "SIMULATED";
    private static final String CHANNEL_ALIPAY_SANDBOX = "ALIPAY_SANDBOX";

    @Resource
    private PaymentOrderMapper paymentOrderMapper;
    @Resource
    private PaymentRecordMapper paymentRecordMapper;
    @Resource
    private TrainingEnrollmentMapper enrollmentMapper;
    @Resource
    private TrainingClassMapper trainingClassMapper;
    @Resource
    private UserMessageMapper userMessageMapper;
    @Resource
    private PaymentProperties paymentProperties;
    @Resource
    private AlipaySandboxGateway alipaySandboxGateway;

    @Override
    public Result<IPage<PaymentOrderVO>> mine(Integer pageNum, Integer pageSize, String keyword, String status) {
        Page<PaymentOrderVO> page = paymentOrderMapper.selectPaymentOrderPage(
                new Page<>(pageNum, pageSize), SecurityUtil.getUserId(), keyword, status);
        return Result.success("查询成功", page);
    }

    @Override
    public Result<IPage<PaymentOrderVO>> manage(Integer pageNum, Integer pageSize, String keyword, String status) {
        Page<PaymentOrderVO> page = paymentOrderMapper.selectPaymentOrderPage(
                new Page<>(pageNum, pageSize), null, keyword, status);
        maskTransactions(page);
        return Result.success("查询成功", page);
    }

    @Override
    public Result<PaymentCapabilitiesVO> capabilities() {
        PaymentCapabilitiesVO capabilities = new PaymentCapabilitiesVO();
        capabilities.setLocalSimulationEnabled(paymentProperties.isLocalSimulationEnabled());
        capabilities.setAlipaySandboxEnabled(paymentProperties.getAlipaySandbox().isEnabled());
        capabilities.setAlipaySandboxConfigured(paymentProperties.isAlipaySandboxConfigured());
        capabilities.setAlipaySandboxAvailable(paymentProperties.isAlipaySandboxAvailable());
        return Result.success("查询成功", capabilities);
    }

    @Override
    @Transactional
    public Result<String> pay(Integer id) {
        if (!paymentProperties.isLocalSimulationEnabled()) {
            throw new ServiceRuntimeException("本地模拟支付未启用");
        }
        PaymentOrder snapshot = requireOwnedOrder(id, false);
        if (PaymentOrderStatus.PAID.name().equals(snapshot.getStatus())) {
            throw new ServiceRuntimeException("该订单已经支付，请勿重复缴费");
        }

        TrainingEnrollment enrollment = enrollmentMapper.selectByIdForUpdate(snapshot.getEnrollmentId());
        PaymentOrder order = requireOwnedOrder(id, true);
        validatePayableOrder(order, enrollment);
        completePayment(order, generateTransactionNo(), order.getAmount(), CHANNEL_SIMULATED, LocalDateTime.now());
        return Result.success("本地模拟缴费成功");
    }

    @Override
    public Result<AlipayPagePayVO> createAlipayPagePay(Integer id) {
        PaymentOrder order = requireOwnedOrder(id, false);
        TrainingEnrollment enrollment = enrollmentMapper.selectById(order.getEnrollmentId());
        validatePayableOrder(order, enrollment);
        TrainingClass trainingClass = trainingClassMapper.selectById(order.getTrainingClassId());
        String className = trainingClass == null ? "培训班次" : trainingClass.getClassName();
        String formHtml = alipaySandboxGateway.createPagePay(order, className + "-培训缴费");
        return Result.success("支付宝沙箱收银台创建成功", new AlipayPagePayVO(order.getOrderNo(), formHtml));
    }

    @Override
    @Transactional
    public Result<AlipayPaymentQueryVO> queryAlipayPayment(String orderNo) {
        PaymentOrder snapshot = paymentOrderMapper.selectByOrderNo(orderNo);
        if (snapshot == null || !SecurityUtil.getUserId().equals(snapshot.getStudentId())) {
            throw new ServiceRuntimeException("缴费订单不存在");
        }
        if (PaymentOrderStatus.PAID.name().equals(snapshot.getStatus())) {
            return Result.success("订单已经支付", queryVO(snapshot, "PAID", "订单已经支付"));
        }
        if (!PaymentOrderStatus.UNPAID.name().equals(snapshot.getStatus())) {
            throw new ServiceRuntimeException("当前订单状态不能查询支付结果");
        }

        AlipayTradeQueryResult gatewayResult = alipaySandboxGateway.query(orderNo);
        if (gatewayResult.isPaid()) {
            PaymentOrder order = paymentOrderMapper.selectByOrderNoForUpdate(orderNo);
            completePayment(order, gatewayResult.getTransactionNo(), gatewayResult.getTotalAmount(),
                    CHANNEL_ALIPAY_SANDBOX, LocalDateTime.now());
            return Result.success("支付宝沙箱缴费成功",
                    queryVO(order, gatewayResult.getTradeStatus(), "支付宝沙箱缴费成功"));
        }

        String message = gatewayResult.isSuccess()
                ? "支付宝订单尚未支付，请完成付款后再次查询"
                : "暂未查询到已支付交易：" + safeMessage(gatewayResult.getMessage());
        return Result.success(message, queryVO(snapshot, gatewayResult.getTradeStatus(), message));
    }

    @Override
    @Transactional
    public boolean handleAlipayNotification(Map<String, String> parameters) {
        AlipayNotification notification = alipaySandboxGateway.verifyNotification(parameters);
        if (!notification.isPaid()) {
            return true;
        }
        PaymentOrder order = paymentOrderMapper.selectByOrderNoForUpdate(notification.getOrderNo());
        if (order == null) {
            throw new ServiceRuntimeException("支付宝回调对应的缴费订单不存在");
        }
        completePayment(order, notification.getTransactionNo(), notification.getTotalAmount(),
                CHANNEL_ALIPAY_SANDBOX, LocalDateTime.now());
        return true;
    }

    @Override
    @Transactional
    public String handleAlipayReturn(Map<String, String> parameters) {
        String frontendUrl = paymentProperties.getAlipaySandbox().getFrontendReturnUrl();
        if (frontendUrl == null || frontendUrl.trim().isEmpty()) {
            frontendUrl = "http://localhost:9527/#/my-payments";
        }
        AlipayNotification returnResult;
        try {
            returnResult = alipaySandboxGateway.verifyReturn(parameters);
        } catch (RuntimeException exception) {
            return appendQuery(frontendUrl, "alipayReturn=invalid");
        }

        PaymentOrder order = paymentOrderMapper.selectByOrderNoForUpdate(returnResult.getOrderNo());
        if (order == null) {
            return appendQuery(frontendUrl, "alipayReturn=invalid");
        }
        completePayment(order, returnResult.getTransactionNo(), returnResult.getTotalAmount(),
                CHANNEL_ALIPAY_SANDBOX, LocalDateTime.now());
        return appendQuery(frontendUrl,
                "alipayReturn=paid&outTradeNo=" + encode(returnResult.getOrderNo()));
    }

    private PaymentOrder requireOwnedOrder(Integer id, boolean lock) {
        PaymentOrder order = lock ? paymentOrderMapper.selectByIdForUpdate(id) : paymentOrderMapper.selectById(id);
        if (order == null || !SecurityUtil.getUserId().equals(order.getStudentId())) {
            throw new ServiceRuntimeException("缴费订单不存在");
        }
        return order;
    }

    private void validatePayableOrder(PaymentOrder order, TrainingEnrollment enrollment) {
        if (PaymentOrderStatus.PAID.name().equals(order.getStatus())) {
            throw new ServiceRuntimeException("该订单已经支付，请勿重复缴费");
        }
        if (!PaymentOrderStatus.UNPAID.name().equals(order.getStatus())) {
            throw new ServiceRuntimeException("当前订单状态不能支付");
        }
        if (enrollment == null || !EnrollmentStatus.ADMITTED.name().equals(enrollment.getStatus())) {
            throw new ServiceRuntimeException("只有已录取且有效的报名可以缴费");
        }
        TrainingClass trainingClass = trainingClassMapper.selectById(order.getTrainingClassId());
        if (trainingClass == null || TrainingClassStatus.CANCELLED.name().equals(trainingClass.getClassStatus())) {
            throw new ServiceRuntimeException("培训班次不存在或已取消，不能缴费");
        }
        if (!LocalDate.now().isBefore(trainingClass.getStartDate())) {
            throw new ServiceRuntimeException("培训已经开始或结束，不能缴费");
        }
    }

    private void completePayment(PaymentOrder order, String transactionNo, BigDecimal paidAmount,
                                 String paymentChannel, LocalDateTime paidAt) {
        if (order == null) {
            throw new ServiceRuntimeException("缴费订单不存在");
        }
        if (paidAmount == null || order.getAmount().compareTo(paidAmount) != 0) {
            throw new ServiceRuntimeException("支付金额与订单金额不一致");
        }
        if (PaymentOrderStatus.PAID.name().equals(order.getStatus())) {
            return;
        }
        if (!PaymentOrderStatus.UNPAID.name().equals(order.getStatus())) {
            throw new ServiceRuntimeException("当前订单状态不能确认支付");
        }
        if (transactionNo == null || transactionNo.trim().isEmpty()) {
            throw new ServiceRuntimeException("支付平台交易流水号不能为空");
        }

        order.setStatus(PaymentOrderStatus.PAID.name());
        order.setPaymentChannel(paymentChannel);
        order.setTransactionNo(transactionNo);
        order.setPaidAt(paidAt);
        order.setUpdateTime(paidAt);
        if (paymentOrderMapper.updateById(order) == 0) {
            throw new ServiceRuntimeException("订单状态已被其他操作修改，请刷新后重试");
        }

        PaymentRecord record = new PaymentRecord();
        record.setPaymentOrderId(order.getId());
        record.setTransactionNo(transactionNo);
        record.setStudentId(order.getStudentId());
        record.setAmount(order.getAmount());
        record.setPaymentMethod(paymentChannel);
        record.setStatus("SUCCESS");
        record.setPaidAt(paidAt);
        record.setCreateTime(paidAt);
        if (paymentRecordMapper.insert(record) == 0) {
            throw new ServiceRuntimeException("保存支付记录失败");
        }

        TrainingClass trainingClass = trainingClassMapper.selectById(order.getTrainingClassId());
        String className = trainingClass == null ? "培训班次" : trainingClass.getClassName();
        String channelLabel = CHANNEL_ALIPAY_SANDBOX.equals(paymentChannel) ? "支付宝沙箱缴费" : "本地模拟缴费";
        UserMessage message = new UserMessage();
        message.setReceiverId(order.getStudentId());
        message.setTitle("培训缴费成功");
        message.setContent("“" + className + "”的" + channelLabel + "已完成，订单号：" + order.getOrderNo());
        message.setMessageType("PAYMENT");
        message.setBusinessType("PAYMENT_ORDER");
        message.setBusinessId(order.getId());
        message.setIsRead(0);
        message.setCreatedBy(order.getStudentId());
        message.setCreateTime(paidAt);
        userMessageMapper.insert(message);
    }

    private AlipayPaymentQueryVO queryVO(PaymentOrder order, String gatewayStatus, String message) {
        return new AlipayPaymentQueryVO(order.getOrderNo(), order.getStatus(), gatewayStatus, message);
    }

    private void maskTransactions(Page<PaymentOrderVO> page) {
        if (page == null || page.getRecords() == null) {
            return;
        }
        page.getRecords().forEach(item -> {
            String value = item.getTransactionNo();
            if (value != null && value.length() > 8) {
                item.setTransactionNo(value.substring(0, 4) + "****" + value.substring(value.length() - 4));
            }
        });
    }

    private String generateTransactionNo() {
        return "LS" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    private String appendQuery(String url, String query) {
        if (url.contains("#")) {
            return url + (url.substring(url.indexOf('#')).contains("?") ? "&" : "?") + query;
        }
        return url + (url.contains("?") ? "&" : "?") + query;
    }

    private String encode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException exception) {
            return value;
        }
    }

    private String safeMessage(String message) {
        return message == null || message.trim().isEmpty() ? "未知状态" : message;
    }
}

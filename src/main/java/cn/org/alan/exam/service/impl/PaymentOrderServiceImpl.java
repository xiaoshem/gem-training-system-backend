package cn.org.alan.exam.service.impl;

import cn.org.alan.exam.common.exception.ServiceRuntimeException;
import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.mapper.PaymentOrderMapper;
import cn.org.alan.exam.mapper.PaymentRecordMapper;
import cn.org.alan.exam.mapper.TrainingClassMapper;
import cn.org.alan.exam.mapper.TrainingEnrollmentMapper;
import cn.org.alan.exam.mapper.UserMessageMapper;
import cn.org.alan.exam.model.entity.PaymentOrder;
import cn.org.alan.exam.model.entity.PaymentRecord;
import cn.org.alan.exam.model.entity.TrainingClass;
import cn.org.alan.exam.model.entity.TrainingEnrollment;
import cn.org.alan.exam.model.entity.UserMessage;
import cn.org.alan.exam.model.enums.EnrollmentStatus;
import cn.org.alan.exam.model.enums.PaymentOrderStatus;
import cn.org.alan.exam.model.enums.TrainingClassStatus;
import cn.org.alan.exam.model.vo.payment.PaymentOrderVO;
import cn.org.alan.exam.service.IPaymentOrderService;
import cn.org.alan.exam.utils.SecurityUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class PaymentOrderServiceImpl extends ServiceImpl<PaymentOrderMapper, PaymentOrder>
        implements IPaymentOrderService {
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

    @Override
    public Result<IPage<PaymentOrderVO>> mine(Integer pageNum, Integer pageSize, String keyword, String status) {
        Page<PaymentOrderVO> page = paymentOrderMapper.selectPaymentOrderPage(
                new Page<>(pageNum, pageSize), SecurityUtil.getUserId(), keyword, status);
        maskTransactions(page);
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
    @Transactional
    public Result<String> pay(Integer id) {
        PaymentOrder snapshot = paymentOrderMapper.selectById(id);
        if (snapshot == null || !SecurityUtil.getUserId().equals(snapshot.getStudentId())) {
            throw new ServiceRuntimeException("缴费订单不存在");
        }
        if (PaymentOrderStatus.PAID.name().equals(snapshot.getStatus())) {
            throw new ServiceRuntimeException("该订单已经支付，请勿重复缴费");
        }
        TrainingEnrollment enrollment = enrollmentMapper.selectByIdForUpdate(snapshot.getEnrollmentId());
        PaymentOrder order = paymentOrderMapper.selectByIdForUpdate(id);
        if (order == null || !SecurityUtil.getUserId().equals(order.getStudentId())) {
            throw new ServiceRuntimeException("缴费订单不存在");
        }
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

        LocalDateTime now = LocalDateTime.now();
        String transactionNo = generateTransactionNo();
        order.setStatus(PaymentOrderStatus.PAID.name());
        order.setTransactionNo(transactionNo);
        order.setPaidAt(now);
        order.setUpdateTime(now);
        if (paymentOrderMapper.updateById(order) == 0) {
            throw new ServiceRuntimeException("订单状态已被其他操作修改，请刷新后重试");
        }

        PaymentRecord record = new PaymentRecord();
        record.setPaymentOrderId(order.getId());
        record.setTransactionNo(transactionNo);
        record.setStudentId(order.getStudentId());
        record.setAmount(order.getAmount());
        record.setPaymentMethod("SIMULATED");
        record.setStatus("SUCCESS");
        record.setPaidAt(now);
        record.setCreateTime(now);
        if (paymentRecordMapper.insert(record) == 0) {
            throw new ServiceRuntimeException("保存支付记录失败");
        }

        UserMessage message = new UserMessage();
        message.setReceiverId(order.getStudentId());
        message.setTitle("培训缴费成功");
        message.setContent("“" + trainingClass.getClassName() + "”的模拟缴费已完成，订单号：" + order.getOrderNo());
        message.setMessageType("PAYMENT");
        message.setBusinessType("PAYMENT_ORDER");
        message.setBusinessId(order.getId());
        message.setIsRead(0);
        message.setCreatedBy(order.getStudentId());
        message.setCreateTime(now);
        userMessageMapper.insert(message);
        return Result.success("模拟缴费成功");
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
}

package cn.org.alan.exam.service.impl;

import cn.org.alan.exam.common.exception.ServiceRuntimeException;
import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.mapper.EnrollmentAuditLogMapper;
import cn.org.alan.exam.mapper.PaymentOrderMapper;
import cn.org.alan.exam.mapper.TrainingClassMapper;
import cn.org.alan.exam.mapper.TrainingEnrollmentMapper;
import cn.org.alan.exam.mapper.UserMessageMapper;
import cn.org.alan.exam.model.entity.EnrollmentAuditLog;
import cn.org.alan.exam.model.entity.PaymentOrder;
import cn.org.alan.exam.model.entity.TrainingClass;
import cn.org.alan.exam.model.entity.TrainingEnrollment;
import cn.org.alan.exam.model.entity.UserMessage;
import cn.org.alan.exam.model.enums.EnrollmentStatus;
import cn.org.alan.exam.model.enums.PaymentOrderStatus;
import cn.org.alan.exam.model.enums.TrainingClassStatus;
import cn.org.alan.exam.model.enums.TrainingPublishStatus;
import cn.org.alan.exam.model.form.enrollment.EnrollmentApplicationForm;
import cn.org.alan.exam.model.form.enrollment.EnrollmentReviewForm;
import cn.org.alan.exam.model.vo.enrollment.EnrollmentVO;
import cn.org.alan.exam.service.IEnrollmentService;
import cn.org.alan.exam.utils.SecurityUtil;
import cn.org.alan.exam.utils.TrainingValidationUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class EnrollmentServiceImpl extends ServiceImpl<TrainingEnrollmentMapper, TrainingEnrollment>
        implements IEnrollmentService {
    @Resource
    private TrainingEnrollmentMapper enrollmentMapper;
    @Resource
    private TrainingClassMapper trainingClassMapper;
    @Resource
    private EnrollmentAuditLogMapper auditLogMapper;
    @Resource
    private PaymentOrderMapper paymentOrderMapper;
    @Resource
    private UserMessageMapper userMessageMapper;

    @Override
    @Transactional
    public Result<String> apply(EnrollmentApplicationForm form) {
        Integer studentId = SecurityUtil.getUserId();
        TrainingClass trainingClass = requireClass(form.getTrainingClassId());
        requireOpenForEnrollment(trainingClass);
        if (enrollmentMapper.selectActiveByStudentAndClass(studentId, trainingClass.getId()) != null) {
            throw new ServiceRuntimeException("你已提交过该班次的有效报名，请勿重复报名");
        }
        Integer admitted = enrollmentMapper.countAdmitted(trainingClass.getId());
        if (admitted != null && admitted >= trainingClass.getCapacity()) {
            throw new ServiceRuntimeException("该培训班次名额已满");
        }

        LocalDateTime now = LocalDateTime.now();
        TrainingEnrollment enrollment = new TrainingEnrollment();
        enrollment.setEnrollmentNo(generateNo("BM"));
        enrollment.setTrainingClassId(trainingClass.getId());
        enrollment.setStudentId(studentId);
        enrollment.setSnapshotRealName(form.getRealName().trim());
        enrollment.setSnapshotIdCard(form.getIdCard().trim().toUpperCase());
        enrollment.setSnapshotOrganization(trimToNull(form.getOrganization()));
        enrollment.setSnapshotPosition(trimToNull(form.getPosition()));
        enrollment.setSnapshotPhone(form.getPhone().trim());
        enrollment.setStatus(EnrollmentStatus.PENDING.name());
        enrollment.setVersion(0);
        enrollment.setCreateTime(now);
        enrollment.setUpdateTime(now);
        try {
            if (enrollmentMapper.insert(enrollment) == 0) {
                throw new ServiceRuntimeException("提交报名失败");
            }
        } catch (DuplicateKeyException exception) {
            throw new ServiceRuntimeException("你已提交过该班次的有效报名，请勿重复报名");
        }
        addAudit(enrollment.getId(), "APPLY", null, EnrollmentStatus.PENDING.name(), null, studentId, now);
        return Result.success("报名申请已提交，请等待管理员审核");
    }

    @Override
    public Result<IPage<EnrollmentVO>> mine(Integer pageNum, Integer pageSize, String keyword, String status) {
        Page<EnrollmentVO> page = enrollmentMapper.selectEnrollmentPage(
                new Page<>(pageNum, pageSize), SecurityUtil.getUserId(), keyword, status, null);
        maskPrivateData(page, true);
        return Result.success("查询成功", page);
    }

    @Override
    public Result<EnrollmentVO> mineByClass(Integer trainingClassId) {
        TrainingEnrollment enrollment = enrollmentMapper.selectActiveByStudentAndClass(
                SecurityUtil.getUserId(), trainingClassId);
        if (enrollment == null) {
            return Result.success("暂无有效报名", null);
        }
        EnrollmentVO vo = enrollmentMapper.selectEnrollmentVOById(enrollment.getId());
        maskPrivateData(vo, true);
        return Result.success("查询成功", vo);
    }

    @Override
    public Result<IPage<EnrollmentVO>> manage(Integer pageNum, Integer pageSize, String keyword,
                                               String status, Integer trainingClassId) {
        Page<EnrollmentVO> page = enrollmentMapper.selectEnrollmentPage(
                new Page<>(pageNum, pageSize), null, keyword, status, trainingClassId);
        maskPrivateData(page, false);
        return Result.success("查询成功", page);
    }

    @Override
    @Transactional
    public Result<String> admit(Integer id, EnrollmentReviewForm form) {
        TrainingEnrollment snapshot = requireEnrollment(id);
        TrainingClass trainingClass = trainingClassMapper.selectByIdForUpdate(snapshot.getTrainingClassId());
        if (trainingClass == null) {
            throw new ServiceRuntimeException("培训班次不存在");
        }
        TrainingEnrollment enrollment = enrollmentMapper.selectByIdForUpdate(id);
        requirePending(enrollment);
        if (!TrainingPublishStatus.PUBLISHED.name().equals(trainingClass.getPublishStatus())
                || TrainingClassStatus.CANCELLED.name().equals(trainingClass.getClassStatus())) {
            throw new ServiceRuntimeException("班次未发布或已取消，不能录取");
        }
        if (!LocalDate.now().isBefore(trainingClass.getStartDate())) {
            throw new ServiceRuntimeException("班次已经开始或结束，不能录取");
        }
        int admitted = enrollmentMapper.countAdmitted(trainingClass.getId());
        if (admitted >= trainingClass.getCapacity()) {
            throw new ServiceRuntimeException("招生名额已满，不能继续录取");
        }

        LocalDateTime now = LocalDateTime.now();
        Integer operatorId = SecurityUtil.getUserId();
        enrollment.setStatus(EnrollmentStatus.ADMITTED.name());
        enrollment.setReviewReason(trimToNull(form == null ? null : form.getReason()));
        enrollment.setReviewedBy(operatorId);
        enrollment.setReviewedAt(now);
        enrollment.setUpdateTime(now);
        updateEnrollment(enrollment);

        PaymentOrder order = new PaymentOrder();
        order.setOrderNo(generateNo("DD"));
        order.setEnrollmentId(enrollment.getId());
        order.setStudentId(enrollment.getStudentId());
        order.setTrainingClassId(trainingClass.getId());
        order.setAmount(trainingClass.getFee());
        order.setStatus(PaymentOrderStatus.UNPAID.name());
        order.setVersion(0);
        order.setCreateTime(now);
        order.setUpdateTime(now);
        if (paymentOrderMapper.insert(order) == 0) {
            throw new ServiceRuntimeException("生成缴费订单失败");
        }

        addAudit(enrollment.getId(), "ADMIT", EnrollmentStatus.PENDING.name(),
                EnrollmentStatus.ADMITTED.name(), enrollment.getReviewReason(), operatorId, now);
        addMessage(enrollment.getStudentId(), "培训报名审核通过",
                "你报名的“" + trainingClass.getClassName() + "”已录取，请前往“我的缴费”完成缴费。",
                enrollment.getId(), operatorId, now);
        return Result.success("录取成功，已生成缴费订单");
    }

    @Override
    @Transactional
    public Result<String> reject(Integer id, EnrollmentReviewForm form) {
        if (form == null || blank(form.getReason())) {
            throw new ServiceRuntimeException("拒绝报名时必须填写原因");
        }
        TrainingEnrollment enrollment = enrollmentMapper.selectByIdForUpdate(id);
        requirePending(enrollment);
        TrainingClass trainingClass = requireClass(enrollment.getTrainingClassId());
        LocalDateTime now = LocalDateTime.now();
        Integer operatorId = SecurityUtil.getUserId();
        enrollment.setStatus(EnrollmentStatus.REJECTED.name());
        enrollment.setReviewReason(form.getReason().trim());
        enrollment.setReviewedBy(operatorId);
        enrollment.setReviewedAt(now);
        enrollment.setUpdateTime(now);
        updateEnrollment(enrollment);
        addAudit(enrollment.getId(), "REJECT", EnrollmentStatus.PENDING.name(),
                EnrollmentStatus.REJECTED.name(), enrollment.getReviewReason(), operatorId, now);
        addMessage(enrollment.getStudentId(), "培训报名审核未通过",
                "你报名的“" + trainingClass.getClassName() + "”未通过审核。原因：" + enrollment.getReviewReason(),
                enrollment.getId(), operatorId, now);
        return Result.success("已拒绝报名并通知学员");
    }

    @Override
    @Transactional
    public Result<String> cancel(Integer id) {
        TrainingEnrollment enrollment = enrollmentMapper.selectByIdForUpdate(id);
        if (enrollment == null || !SecurityUtil.getUserId().equals(enrollment.getStudentId())) {
            throw new ServiceRuntimeException("报名记录不存在");
        }
        if (!EnrollmentStatus.PENDING.name().equals(enrollment.getStatus())
                && !EnrollmentStatus.ADMITTED.name().equals(enrollment.getStatus())) {
            throw new ServiceRuntimeException("当前报名状态不能取消");
        }
        TrainingClass trainingClass = requireClass(enrollment.getTrainingClassId());
        if (!LocalDate.now().isBefore(trainingClass.getStartDate())) {
            throw new ServiceRuntimeException("培训已经开始或结束，不能取消报名");
        }

        PaymentOrder order = paymentOrderMapper.selectByEnrollmentIdForUpdate(enrollment.getId());
        if (order != null && PaymentOrderStatus.PAID.name().equals(order.getStatus())) {
            throw new ServiceRuntimeException("该报名已缴费，不能直接取消，请联系管理员");
        }
        LocalDateTime now = LocalDateTime.now();
        String fromStatus = enrollment.getStatus();
        enrollment.setStatus(EnrollmentStatus.CANCELLED.name());
        enrollment.setCancelledAt(now);
        enrollment.setUpdateTime(now);
        updateEnrollment(enrollment);
        if (order != null && PaymentOrderStatus.UNPAID.name().equals(order.getStatus())) {
            order.setStatus(PaymentOrderStatus.CANCELLED.name());
            order.setUpdateTime(now);
            paymentOrderMapper.updateById(order);
        }
        addAudit(enrollment.getId(), "CANCEL", fromStatus, EnrollmentStatus.CANCELLED.name(),
                "学员主动取消报名", enrollment.getStudentId(), now);
        return Result.success("报名已取消");
    }

    private TrainingClass requireClass(Integer id) {
        TrainingClass trainingClass = trainingClassMapper.selectById(id);
        if (trainingClass == null) {
            throw new ServiceRuntimeException("培训班次不存在");
        }
        return trainingClass;
    }

    private TrainingEnrollment requireEnrollment(Integer id) {
        TrainingEnrollment enrollment = enrollmentMapper.selectById(id);
        if (enrollment == null) {
            throw new ServiceRuntimeException("报名记录不存在");
        }
        return enrollment;
    }

    private void requireOpenForEnrollment(TrainingClass trainingClass) {
        LocalDateTime now = LocalDateTime.now();
        String currentStatus = TrainingValidationUtil.deriveClassStatus(trainingClass, now);
        if (!TrainingPublishStatus.PUBLISHED.name().equals(trainingClass.getPublishStatus())
                || !TrainingClassStatus.ENROLLING.name().equals(currentStatus)) {
            throw new ServiceRuntimeException("当前不在该班次的报名时间内");
        }
    }

    private void requirePending(TrainingEnrollment enrollment) {
        if (enrollment == null) {
            throw new ServiceRuntimeException("报名记录不存在");
        }
        if (!EnrollmentStatus.PENDING.name().equals(enrollment.getStatus())) {
            throw new ServiceRuntimeException("只有待审核报名可以处理");
        }
    }

    private void updateEnrollment(TrainingEnrollment enrollment) {
        if (enrollmentMapper.updateById(enrollment) == 0) {
            throw new ServiceRuntimeException("报名状态已被其他操作修改，请刷新后重试");
        }
    }

    private void addAudit(Integer enrollmentId, String action, String fromStatus, String toStatus,
                          String reason, Integer operatorId, LocalDateTime now) {
        EnrollmentAuditLog log = new EnrollmentAuditLog();
        log.setEnrollmentId(enrollmentId);
        log.setAction(action);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setReason(reason);
        log.setOperatorId(operatorId);
        log.setCreateTime(now);
        auditLogMapper.insert(log);
    }

    private void addMessage(Integer receiverId, String title, String content, Integer businessId,
                            Integer operatorId, LocalDateTime now) {
        UserMessage message = new UserMessage();
        message.setReceiverId(receiverId);
        message.setTitle(title);
        message.setContent(content);
        message.setMessageType("ENROLLMENT");
        message.setBusinessType("TRAINING_ENROLLMENT");
        message.setBusinessId(businessId);
        message.setIsRead(0);
        message.setCreatedBy(operatorId);
        message.setCreateTime(now);
        userMessageMapper.insert(message);
    }

    private void maskPrivateData(Page<EnrollmentVO> page, boolean hidePhone) {
        if (page != null && page.getRecords() != null) {
            page.getRecords().forEach(item -> maskPrivateData(item, hidePhone));
        }
    }

    private void maskPrivateData(EnrollmentVO vo, boolean hidePhone) {
        if (vo == null) {
            return;
        }
        vo.setIdCard(maskIdCard(vo.getIdCard()));
        if (hidePhone) {
            vo.setPhone(maskPhone(vo.getPhone()));
        }
        vo.setTransactionNo(maskTransaction(vo.getTransactionNo()));
    }

    private String maskIdCard(String value) {
        return value != null && value.length() >= 10
                ? value.substring(0, 6) + "********" + value.substring(value.length() - 4) : value;
    }

    private String maskPhone(String value) {
        return value != null && value.length() >= 7
                ? value.substring(0, 3) + "****" + value.substring(value.length() - 4) : value;
    }

    private String maskTransaction(String value) {
        return value != null && value.length() > 8
                ? value.substring(0, 4) + "****" + value.substring(value.length() - 4) : value;
    }

    private String generateNo(String prefix) {
        return prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    private String trimToNull(String value) {
        return blank(value) ? null : value.trim();
    }

    private boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

package cn.org.alan.exam.service.impl;

import cn.org.alan.exam.mapper.CertificateUserMapper;
import cn.org.alan.exam.mapper.ExamMapper;
import cn.org.alan.exam.mapper.UserExamsScoreMapper;
import cn.org.alan.exam.model.entity.CertificateUser;
import cn.org.alan.exam.model.entity.Exam;
import cn.org.alan.exam.model.entity.UserExamsScore;
import cn.org.alan.exam.utils.ClassTokenGenerator;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * 证书发放服务。
 *
 * 所有发证入口都必须经过这里，避免含简答题的考试在完成批改前，
 * 或最终成绩未达到及格分时提前发放证书。
 */
@Service
public class CertificateIssuanceService {

    @Resource
    private ExamMapper examMapper;
    @Resource
    private UserExamsScoreMapper userExamsScoreMapper;
    @Resource
    private CertificateUserMapper certificateUserMapper;

    @Transactional
    public boolean issueIfEligible(Integer examId, Integer userId) {
        if (examId == null || userId == null) {
            return false;
        }

        Exam exam = examMapper.selectById(examId);
        if (exam == null
                || exam.getCertificateId() == null
                || exam.getCertificateId() <= 0
                || exam.getPassedScore() == null) {
            return false;
        }

        LambdaQueryWrapper<UserExamsScore> scoreWrapper = new LambdaQueryWrapper<UserExamsScore>()
                .eq(UserExamsScore::getExamId, examId)
                .eq(UserExamsScore::getUserId, userId)
                .last("limit 1");
        UserExamsScore score = userExamsScoreMapper.selectOne(scoreWrapper);

        if (score == null
                || !Integer.valueOf(1).equals(score.getState())
                || score.getUserScore() == null
                || score.getUserScore() < exam.getPassedScore()) {
            return false;
        }

        // 含简答题必须是 whetherMark=1（已批改），无简答题必须是
        // whetherMark=-1（无需批改），成绩才算最终确认。
        boolean hasSaq = exam.getSaqCount() != null && exam.getSaqCount() > 0;
        boolean finalScoreConfirmed = hasSaq
                ? Integer.valueOf(1).equals(score.getWhetherMark())
                : Integer.valueOf(-1).equals(score.getWhetherMark());
        if (!finalScoreConfirmed) {
            return false;
        }

        LambdaQueryWrapper<CertificateUser> certificateWrapper = new LambdaQueryWrapper<CertificateUser>()
                .eq(CertificateUser::getExamId, examId)
                .eq(CertificateUser::getUserId, userId)
                .last("limit 1");
        if (certificateUserMapper.selectOne(certificateWrapper) != null) {
            return true;
        }

        CertificateUser certificateUser = new CertificateUser();
        certificateUser.setCertificateId(exam.getCertificateId());
        certificateUser.setUserId(userId);
        certificateUser.setExamId(examId);
        certificateUser.setCode(ClassTokenGenerator.generateClassToken(18));
        return certificateUserMapper.insert(certificateUser) > 0;
    }
}

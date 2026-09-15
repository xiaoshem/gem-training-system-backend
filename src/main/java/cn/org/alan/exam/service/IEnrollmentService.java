package cn.org.alan.exam.service;

import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.model.entity.TrainingEnrollment;
import cn.org.alan.exam.model.form.enrollment.EnrollmentApplicationForm;
import cn.org.alan.exam.model.form.enrollment.EnrollmentReviewForm;
import cn.org.alan.exam.model.vo.enrollment.EnrollmentVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

public interface IEnrollmentService extends IService<TrainingEnrollment> {
    Result<String> apply(EnrollmentApplicationForm form);
    Result<IPage<EnrollmentVO>> mine(Integer pageNum, Integer pageSize, String keyword, String status);
    Result<EnrollmentVO> mineByClass(Integer trainingClassId);
    Result<IPage<EnrollmentVO>> manage(Integer pageNum, Integer pageSize, String keyword,
                                        String status, Integer trainingClassId);
    Result<String> admit(Integer id, EnrollmentReviewForm form);
    Result<String> reject(Integer id, EnrollmentReviewForm form);
    Result<String> cancel(Integer id);
}

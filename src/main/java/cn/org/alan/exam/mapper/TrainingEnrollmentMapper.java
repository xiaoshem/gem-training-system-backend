package cn.org.alan.exam.mapper;

import cn.org.alan.exam.model.entity.TrainingEnrollment;
import cn.org.alan.exam.model.vo.enrollment.EnrollmentVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;

public interface TrainingEnrollmentMapper extends BaseMapper<TrainingEnrollment> {
    Page<EnrollmentVO> selectEnrollmentPage(Page<EnrollmentVO> page,
                                             @Param("studentId") Integer studentId,
                                             @Param("keyword") String keyword,
                                             @Param("status") String status,
                                             @Param("trainingClassId") Integer trainingClassId);

    EnrollmentVO selectEnrollmentVOById(@Param("id") Integer id);

    TrainingEnrollment selectByIdForUpdate(@Param("id") Integer id);

    TrainingEnrollment selectActiveByStudentAndClass(@Param("studentId") Integer studentId,
                                                       @Param("trainingClassId") Integer trainingClassId);

    Integer countAdmitted(@Param("trainingClassId") Integer trainingClassId);
}

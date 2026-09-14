package cn.org.alan.exam.mapper;

import cn.org.alan.exam.model.entity.TrainingClass;
import cn.org.alan.exam.model.vo.training.TrainingClassVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;

public interface TrainingClassMapper extends BaseMapper<TrainingClass> {
    Page<TrainingClassVO> selectTrainingClassPage(Page<TrainingClassVO> page,
                                                  @Param("keyword") String keyword,
                                                  @Param("classStatus") String classStatus,
                                                  @Param("publishStatus") String publishStatus,
                                                  @Param("instructorId") Integer instructorId,
                                                  @Param("publishedOnly") Boolean publishedOnly);

    TrainingClassVO selectTrainingClassVOById(@Param("id") Integer id);

    TrainingClass selectByIdForUpdate(@Param("id") Integer id);
}

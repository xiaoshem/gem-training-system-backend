package cn.org.alan.exam.mapper;

import cn.org.alan.exam.model.entity.TrainingSession;
import cn.org.alan.exam.model.vo.training.TrainingSessionVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TrainingSessionMapper extends BaseMapper<TrainingSession> {
    List<TrainingSessionVO> selectByTrainingClassId(@Param("trainingClassId") Integer trainingClassId);
}

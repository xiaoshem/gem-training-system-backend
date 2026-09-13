package cn.org.alan.exam.service;

import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.model.entity.TrainingSession;
import cn.org.alan.exam.model.form.training.TrainingSessionForm;
import cn.org.alan.exam.model.vo.training.TrainingSessionVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface ITrainingSessionService extends IService<TrainingSession> {
    Result<List<TrainingSessionVO>> listByClass(Integer trainingClassId);
    Result<List<TrainingSessionVO>> publicListByClass(Integer trainingClassId);
    Result<String> add(TrainingSessionForm form);
    Result<String> update(Integer id, TrainingSessionForm form);
    Result<String> delete(Integer id);
}

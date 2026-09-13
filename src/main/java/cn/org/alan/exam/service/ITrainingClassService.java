package cn.org.alan.exam.service;

import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.model.entity.TrainingClass;
import cn.org.alan.exam.model.form.training.TrainingClassForm;
import cn.org.alan.exam.model.vo.training.TrainingClassVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

public interface ITrainingClassService extends IService<TrainingClass> {
    Result<IPage<TrainingClassVO>> paging(Integer pageNum, Integer pageSize, String keyword,
                                          String classStatus, String publishStatus);
    Result<IPage<TrainingClassVO>> available(Integer pageNum, Integer pageSize, String keyword);
    Result<TrainingClassVO> detail(Integer id);
    Result<TrainingClassVO> publicDetail(Integer id);
    Result<String> add(TrainingClassForm form);
    Result<String> update(Integer id, TrainingClassForm form);
    Result<String> delete(Integer id);
    Result<String> publish(Integer id);
    Result<String> unpublish(Integer id);
    Result<String> cancel(Integer id);
}

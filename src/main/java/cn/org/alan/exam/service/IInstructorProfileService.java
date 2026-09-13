package cn.org.alan.exam.service;

import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.model.entity.InstructorProfile;
import cn.org.alan.exam.model.form.training.InstructorProfileForm;
import cn.org.alan.exam.model.vo.training.InstructorProfileVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface IInstructorProfileService extends IService<InstructorProfile> {
    Result<IPage<InstructorProfileVO>> paging(Integer pageNum, Integer pageSize, String keyword, String status);
    Result<List<InstructorProfileVO>> options();
    Result<List<InstructorProfileVO>> candidates();
    Result<String> add(InstructorProfileForm form);
    Result<String> update(Integer id, InstructorProfileForm form);
    Result<String> delete(Integer id);
}

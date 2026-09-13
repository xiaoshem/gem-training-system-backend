package cn.org.alan.exam.mapper;

import cn.org.alan.exam.model.entity.InstructorProfile;
import cn.org.alan.exam.model.vo.training.InstructorProfileVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface InstructorProfileMapper extends BaseMapper<InstructorProfile> {
    Page<InstructorProfileVO> selectInstructorPage(Page<InstructorProfileVO> page,
                                                    @Param("keyword") String keyword,
                                                    @Param("status") String status);

    List<InstructorProfileVO> selectEnabledOptions();

    List<InstructorProfileVO> selectTeacherCandidates();
}

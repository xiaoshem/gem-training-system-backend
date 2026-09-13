package cn.org.alan.exam.service.impl;

import cn.org.alan.exam.mapper.CourseInstructorMapper;
import cn.org.alan.exam.mapper.CourseMapper;
import cn.org.alan.exam.mapper.InstructorProfileMapper;
import cn.org.alan.exam.mapper.TrainingClassMapper;
import cn.org.alan.exam.mapper.TrainingSessionMapper;
import cn.org.alan.exam.mapper.UserMapper;
import cn.org.alan.exam.model.entity.InstructorProfile;
import cn.org.alan.exam.model.entity.User;
import cn.org.alan.exam.model.form.training.InstructorProfileForm;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class InstructorProfileServiceImplTest {

    @InjectMocks
    private InstructorProfileServiceImpl service;
    @Mock
    private InstructorProfileMapper instructorProfileMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private CourseMapper courseMapper;
    @Mock
    private CourseInstructorMapper courseInstructorMapper;
    @Mock
    private TrainingClassMapper trainingClassMapper;
    @Mock
    private TrainingSessionMapper trainingSessionMapper;

    @Test
    public void updateSynchronizesTeacherOrganizationPositionAndPhone() {
        InstructorProfile profile = new InstructorProfile();
        profile.setId(3);
        profile.setUserId(168);
        profile.setStatus("ENABLED");
        when(instructorProfileMapper.selectById(3)).thenReturn(profile);

        User teacher = new User();
        teacher.setId(168);
        teacher.setRoleId(2);
        teacher.setStatus(1);
        when(userMapper.selectById(168)).thenReturn(teacher);
        when(instructorProfileMapper.selectCount(any())).thenReturn(0L);
        when(instructorProfileMapper.updateById(any(InstructorProfile.class))).thenReturn(1);
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        InstructorProfileForm form = new InstructorProfileForm();
        form.setUserId(168);
        form.setOrganization(" 梧州市人工宝石产业实训中心 ");
        form.setPosition(" 智能加工实训讲师 ");
        form.setPhone("13800138000");
        form.setStatus("ENABLED");

        service.update(3, form);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(userCaptor.capture());
        assertEquals("梧州市人工宝石产业实训中心", userCaptor.getValue().getOrganization());
        assertEquals("智能加工实训讲师", userCaptor.getValue().getPosition());
        assertEquals("13800138000", userCaptor.getValue().getPhone());
    }
}

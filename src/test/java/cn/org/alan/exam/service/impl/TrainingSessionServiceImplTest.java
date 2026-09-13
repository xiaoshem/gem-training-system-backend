package cn.org.alan.exam.service.impl;

import cn.org.alan.exam.common.exception.ServiceRuntimeException;
import cn.org.alan.exam.mapper.TrainingClassMapper;
import cn.org.alan.exam.mapper.TrainingSessionMapper;
import cn.org.alan.exam.model.entity.TrainingClass;
import cn.org.alan.exam.model.entity.TrainingSession;
import cn.org.alan.exam.model.enums.TrainingClassStatus;
import cn.org.alan.exam.model.enums.TrainingPublishStatus;
import cn.org.alan.exam.model.form.training.TrainingSessionForm;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalDate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TrainingSessionServiceImplTest {

    @InjectMocks
    private TrainingSessionServiceImpl service;
    @Mock
    private TrainingClassMapper trainingClassMapper;
    @Mock
    private TrainingSessionMapper trainingSessionMapper;

    @Test
    public void addRejectsSessionMaintenanceForExpiredDraft() {
        TrainingClass trainingClass = new TrainingClass();
        trainingClass.setId(9);
        trainingClass.setPublishStatus(TrainingPublishStatus.DRAFT.name());
        trainingClass.setClassStatus(TrainingClassStatus.PLANNED.name());
        trainingClass.setStartDate(LocalDate.now().minusDays(2));
        trainingClass.setEndDate(LocalDate.now().minusDays(1));
        when(trainingClassMapper.selectById(9)).thenReturn(trainingClass);

        TrainingSessionForm form = new TrainingSessionForm();
        form.setTrainingClassId(9);
        try {
            service.add(form);
            fail("过期草稿不应该允许维护课次");
        } catch (ServiceRuntimeException exception) {
            assertEquals("只有尚未开课且未取消的草稿班次可以维护课次", exception.getMessage());
        }

        verify(trainingSessionMapper, never()).insert(any(TrainingSession.class));
    }
}

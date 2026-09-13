package cn.org.alan.exam.utils;

import cn.org.alan.exam.common.exception.ServiceRuntimeException;
import cn.org.alan.exam.model.entity.TrainingClass;
import cn.org.alan.exam.model.enums.TrainingClassStatus;
import cn.org.alan.exam.model.enums.TrainingPublishStatus;
import cn.org.alan.exam.model.form.training.TrainingClassForm;
import cn.org.alan.exam.model.form.training.TrainingSessionForm;
import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.Assert.assertEquals;

public class TrainingValidationUtilTest {

    @Test(expected = ServiceRuntimeException.class)
    public void shouldRejectEnrollmentEndingAfterClassStarts() {
        TrainingClassForm form = new TrainingClassForm();
        form.setEnrollmentStart(LocalDateTime.of(2026, 9, 1, 8, 0));
        form.setEnrollmentEnd(LocalDateTime.of(2026, 9, 11, 18, 0));
        form.setStartDate(LocalDate.of(2026, 9, 10));
        form.setEndDate(LocalDate.of(2026, 9, 20));

        TrainingValidationUtil.validateClassDates(form);
    }

    @Test(expected = ServiceRuntimeException.class)
    public void shouldRejectSessionOutsideClassDates() {
        TrainingClass trainingClass = new TrainingClass();
        trainingClass.setStartDate(LocalDate.of(2026, 10, 1));
        trainingClass.setEndDate(LocalDate.of(2026, 10, 5));
        TrainingSessionForm form = new TrainingSessionForm();
        form.setStartTime(LocalDateTime.of(2026, 10, 6, 9, 0));
        form.setEndTime(LocalDateTime.of(2026, 10, 6, 11, 0));

        TrainingValidationUtil.validateSessionDates(form, trainingClass);
    }

    @Test
    public void shouldDeriveEnrollmentStatus() {
        TrainingClass trainingClass = new TrainingClass();
        trainingClass.setClassStatus(TrainingClassStatus.PLANNED.name());
        trainingClass.setPublishStatus(TrainingPublishStatus.PUBLISHED.name());
        trainingClass.setEnrollmentStart(LocalDateTime.of(2026, 9, 1, 0, 0));
        trainingClass.setEnrollmentEnd(LocalDateTime.of(2026, 9, 30, 23, 59));
        trainingClass.setStartDate(LocalDate.of(2026, 10, 1));
        trainingClass.setEndDate(LocalDate.of(2026, 10, 5));

        String status = TrainingValidationUtil.deriveClassStatus(
                trainingClass, LocalDateTime.of(2026, 9, 12, 10, 0));

        assertEquals(TrainingClassStatus.ENROLLING.name(), status);
    }

    @Test
    public void shouldKeepFutureDraftInactiveDuringEnrollmentDates() {
        TrainingClass trainingClass = new TrainingClass();
        trainingClass.setClassStatus(TrainingClassStatus.PLANNED.name());
        trainingClass.setPublishStatus(TrainingPublishStatus.DRAFT.name());
        trainingClass.setEnrollmentStart(LocalDateTime.of(2026, 9, 1, 0, 0));
        trainingClass.setEnrollmentEnd(LocalDateTime.of(2026, 9, 30, 23, 59));
        trainingClass.setStartDate(LocalDate.of(2026, 10, 1));
        trainingClass.setEndDate(LocalDate.of(2026, 10, 5));

        String status = TrainingValidationUtil.deriveClassStatus(
                trainingClass, LocalDateTime.of(2026, 9, 12, 10, 0));

        assertEquals(TrainingClassStatus.PLANNED.name(), status);
    }

    @Test
    public void shouldMarkStartedDraftAsExpired() {
        TrainingClass trainingClass = new TrainingClass();
        trainingClass.setClassStatus(TrainingClassStatus.PLANNED.name());
        trainingClass.setPublishStatus(TrainingPublishStatus.DRAFT.name());
        trainingClass.setEnrollmentStart(LocalDateTime.of(2026, 8, 1, 0, 0));
        trainingClass.setEnrollmentEnd(LocalDateTime.of(2026, 8, 31, 23, 59));
        trainingClass.setStartDate(LocalDate.of(2026, 9, 10));
        trainingClass.setEndDate(LocalDate.of(2026, 9, 20));

        String status = TrainingValidationUtil.deriveClassStatus(
                trainingClass, LocalDateTime.of(2026, 9, 12, 10, 0));

        assertEquals(TrainingClassStatus.EXPIRED.name(), status);
    }
}

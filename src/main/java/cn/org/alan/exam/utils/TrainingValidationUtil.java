package cn.org.alan.exam.utils;

import cn.org.alan.exam.common.exception.ServiceRuntimeException;
import cn.org.alan.exam.model.entity.TrainingClass;
import cn.org.alan.exam.model.enums.TrainingClassStatus;
import cn.org.alan.exam.model.enums.TrainingPublishStatus;
import cn.org.alan.exam.model.form.training.TrainingClassForm;
import cn.org.alan.exam.model.form.training.TrainingSessionForm;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class TrainingValidationUtil {

    private TrainingValidationUtil() {
    }

    public static void validateClassDates(TrainingClassForm form) {
        if (!form.getEnrollmentStart().isBefore(form.getEnrollmentEnd())) {
            throw new ServiceRuntimeException("报名开始时间必须早于报名结束时间");
        }
        if (form.getStartDate().isAfter(form.getEndDate())) {
            throw new ServiceRuntimeException("开课日期不能晚于结课日期");
        }
        if (form.getEnrollmentEnd().toLocalDate().isAfter(form.getStartDate())) {
            throw new ServiceRuntimeException("报名结束日期不能晚于开课日期");
        }
    }

    public static void validateSessionDates(TrainingSessionForm form, TrainingClass trainingClass) {
        if (!form.getStartTime().isBefore(form.getEndTime())) {
            throw new ServiceRuntimeException("课次开始时间必须早于结束时间");
        }
        LocalDate sessionStartDate = form.getStartTime().toLocalDate();
        LocalDate sessionEndDate = form.getEndTime().toLocalDate();
        if (sessionStartDate.isBefore(trainingClass.getStartDate())
                || sessionEndDate.isAfter(trainingClass.getEndDate())) {
            throw new ServiceRuntimeException("课次时间必须在培训班次起止日期内");
        }
    }

    public static String deriveClassStatus(TrainingClass trainingClass, LocalDateTime now) {
        if (TrainingClassStatus.CANCELLED.name().equals(trainingClass.getClassStatus())) {
            return TrainingClassStatus.CANCELLED.name();
        }
        LocalDate today = now.toLocalDate();
        if (TrainingPublishStatus.DRAFT.name().equals(trainingClass.getPublishStatus())) {
            return today.isBefore(trainingClass.getStartDate())
                    ? TrainingClassStatus.PLANNED.name()
                    : TrainingClassStatus.EXPIRED.name();
        }
        if (today.isAfter(trainingClass.getEndDate())) {
            return TrainingClassStatus.COMPLETED.name();
        }
        if (!today.isBefore(trainingClass.getStartDate()) && !today.isAfter(trainingClass.getEndDate())) {
            return TrainingClassStatus.IN_PROGRESS.name();
        }
        if (now.isBefore(trainingClass.getEnrollmentStart())) {
            return TrainingClassStatus.PLANNED.name();
        }
        if (!now.isAfter(trainingClass.getEnrollmentEnd())) {
            return TrainingClassStatus.ENROLLING.name();
        }
        return TrainingClassStatus.UPCOMING.name();
    }
}

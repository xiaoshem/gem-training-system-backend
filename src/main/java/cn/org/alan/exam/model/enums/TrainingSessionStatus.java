package cn.org.alan.exam.model.enums;

public enum TrainingSessionStatus {
    PLANNED,
    COMPLETED,
    CANCELLED;

    public static boolean supports(String value) {
        for (TrainingSessionStatus status : values()) {
            if (status.name().equals(value)) {
                return true;
            }
        }
        return false;
    }
}

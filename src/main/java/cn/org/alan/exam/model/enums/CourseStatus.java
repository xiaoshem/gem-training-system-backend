package cn.org.alan.exam.model.enums;

public enum CourseStatus {
    ENABLED,
    DISABLED;

    public static boolean supports(String value) {
        for (CourseStatus status : values()) {
            if (status.name().equals(value)) {
                return true;
            }
        }
        return false;
    }
}

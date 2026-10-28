package kz.delivery.model;

public final class Validation {
    private Validation() {}

    public static String text(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    public static double nonNegative(double value, String field) {
        if (!Double.isFinite(value) || value < 0) {
            throw new IllegalArgumentException(field + " must be finite and non-negative");
        }
        return value;
    }

    public static <T> T required(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }
}

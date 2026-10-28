package kz.delivery.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class NoSuitableRouteException extends RuntimeException {
    private final Map<String, List<String>> rejectedRoutes;

    public NoSuitableRouteException(String message, Map<String, List<String>> rejectedRoutes) {
        super(message);
        Map<String, List<String>> reasons = new LinkedHashMap<>();
        rejectedRoutes.forEach((id, values) -> reasons.put(id, List.copyOf(values)));
        this.rejectedRoutes = Collections.unmodifiableMap(reasons);
    }

    public Map<String, List<String>> getRejectedRoutes() {
        return rejectedRoutes;
    }
}

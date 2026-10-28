package kz.delivery.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record RouteDecision(
        Route route,
        String strategyName,
        double score,
        RouteMetrics metrics,
        List<String> explanation,
        Map<String, Double> candidateScores,
        Map<String, List<String>> rejectedRoutes
) {
    public RouteDecision {
        Validation.required(route, "Selected route");
        Validation.text(strategyName, "Strategy name");
        Validation.nonNegative(score, "Score");
        Validation.required(metrics, "Metrics");
        explanation = List.copyOf(explanation);
        candidateScores = Collections.unmodifiableMap(new LinkedHashMap<>(candidateScores));
        Map<String, List<String>> reasons = new LinkedHashMap<>();
        rejectedRoutes.forEach((id, values) -> reasons.put(id, List.copyOf(values)));
        rejectedRoutes = Collections.unmodifiableMap(reasons);
    }
}

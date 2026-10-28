package kz.delivery.strategy;

import kz.delivery.model.DeliveryRequest;
import kz.delivery.model.Route;
import kz.delivery.model.RouteDecision;
import kz.delivery.model.Validation;
import kz.delivery.service.RouteConstraints;
import kz.delivery.service.RouteConstraints.Candidate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.ToDoubleFunction;

public abstract class ScoringRouteStrategy implements RouteStrategy {
    @Override
    public final RouteDecision selectRoute(List<Route> routes, DeliveryRequest request) {
        RouteConstraints.Evaluation evaluation = RouteConstraints.evaluate(routes, request);
        ToDoubleFunction<Candidate> scoring = scoringFunction(evaluation.candidates());
        Map<String, Double> scores = new LinkedHashMap<>();
        for (Candidate candidate : evaluation.candidates()) {
            double score = scoring.applyAsDouble(candidate);
            Validation.nonNegative(score, "Strategy score");
            scores.put(candidate.route().id(), score);
        }
        Candidate selected = evaluation.candidates().stream()
                .min(Comparator.<Candidate>comparingDouble(c -> scores.get(c.route().id()))
                        .thenComparing(c -> c.route().id()))
                .orElseThrow();

        List<String> explanation = new ArrayList<>(explain(selected, request));
        explanation.add(String.format(Locale.ROOT,
                "Lowest score: %.4f; equal scores are resolved by route id", scores.get(selected.route().id())));
        return new RouteDecision(selected.route(), getStrategyName(), scores.get(selected.route().id()),
                selected.metrics(), explanation, scores, evaluation.rejectedRoutes());
    }

    protected abstract ToDoubleFunction<Candidate> scoringFunction(List<Candidate> candidates);

    protected abstract List<String> explain(Candidate selected, DeliveryRequest request);
}

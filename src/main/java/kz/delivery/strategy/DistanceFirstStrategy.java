package kz.delivery.strategy;

import kz.delivery.model.DeliveryRequest;
import kz.delivery.service.RouteConstraints.Candidate;

import java.util.List;
import java.util.Locale;
import java.util.function.ToDoubleFunction;

public final class DistanceFirstStrategy extends ScoringRouteStrategy {
    @Override
    public String getStrategyName() {
        return "Distance first";
    }

    @Override
    protected ToDoubleFunction<Candidate> scoringFunction(List<Candidate> candidates) {
        return candidate -> candidate.route().distanceKm();
    }

    @Override
    protected List<String> explain(Candidate selected, DeliveryRequest request) {
        return List.of(String.format(Locale.ROOT,
                "Shortest feasible distance: %.1f km", selected.route().distanceKm()));
    }
}

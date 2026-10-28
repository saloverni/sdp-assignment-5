package kz.delivery.strategy;

import kz.delivery.model.DeliveryRequest;
import kz.delivery.model.Route;
import kz.delivery.service.RouteConstraints.Candidate;

import java.util.List;
import java.util.Locale;
import java.util.function.ToDoubleFunction;

public final class FastestRouteStrategy extends ScoringRouteStrategy {
    @Override
    public String getStrategyName() {
        return "Fastest";
    }

    @Override
    protected ToDoubleFunction<Candidate> scoringFunction(List<Candidate> candidates) {
        return candidate -> candidate.metrics().deliveryMinutes();
    }

    @Override
    protected List<String> explain(Candidate selected, DeliveryRequest request) {
        Route route = selected.route();
        return List.of(String.format(Locale.ROOT,
                "Time: %d base + %d traffic + %d weather = %.0f min",
                route.estimatedMinutes(), route.trafficDelayMinutes(), route.weatherDelayMinutes(),
                selected.metrics().deliveryMinutes()));
    }
}

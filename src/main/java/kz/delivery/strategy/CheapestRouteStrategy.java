package kz.delivery.strategy;

import kz.delivery.model.DeliveryPriority;
import kz.delivery.model.DeliveryRequest;
import kz.delivery.model.Route;
import kz.delivery.model.RouteMetrics;
import kz.delivery.service.RouteConstraints.Candidate;

import java.util.List;
import java.util.Locale;
import java.util.function.ToDoubleFunction;

public final class CheapestRouteStrategy extends ScoringRouteStrategy {
    @Override
    public String getStrategyName() {
        return "Cheapest";
    }

    @Override
    protected ToDoubleFunction<Candidate> scoringFunction(List<Candidate> candidates) {
        return candidate -> candidate.metrics().totalCost();
    }

    @Override
    protected List<String> explain(Candidate selected, DeliveryRequest request) {
        Route route = selected.route();
        double surcharge = request.priority() == DeliveryPriority.URGENT ? RouteMetrics.URGENT_SURCHARGE : 0;
        return List.of(String.format(Locale.ROOT,
                "Cost: %.2f base + %.1f km * %.2f + %.2f fees + %.2f priority = %.2f KZT",
                route.cost(), route.distanceKm(), RouteMetrics.COST_PER_KM, route.additionalFees(),
                surcharge, selected.metrics().totalCost()));
    }
}

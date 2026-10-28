package kz.delivery.strategy;

import kz.delivery.model.DeliveryRequest;
import kz.delivery.model.Route;
import kz.delivery.service.RouteConstraints.Candidate;

import java.util.List;
import java.util.Locale;
import java.util.function.ToDoubleFunction;

public final class EcoFriendlyRouteStrategy extends ScoringRouteStrategy {
    private static final double EMISSION_WEIGHT = 0.8;
    private static final double DISTANCE_WEIGHT = 0.2;

    @Override
    public String getStrategyName() {
        return "Eco friendly";
    }

    @Override
    protected ToDoubleFunction<Candidate> scoringFunction(List<Candidate> candidates) {
        return candidate -> candidate.route().carbonEmission() * EMISSION_WEIGHT
                + candidate.route().distanceKm() * DISTANCE_WEIGHT;
    }

    @Override
    protected List<String> explain(Candidate selected, DeliveryRequest request) {
        Route route = selected.route();
        return List.of(String.format(Locale.ROOT,
                "Eco score: %.2f kg CO2 * %.1f + %.1f km * %.1f",
                route.carbonEmission(), EMISSION_WEIGHT, route.distanceKm(), DISTANCE_WEIGHT));
    }
}

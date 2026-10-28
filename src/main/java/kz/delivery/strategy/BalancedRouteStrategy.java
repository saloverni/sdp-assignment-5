package kz.delivery.strategy;

import kz.delivery.model.DeliveryRequest;
import kz.delivery.model.Validation;
import kz.delivery.service.RouteConstraints.Candidate;

import java.util.List;
import java.util.Locale;
import java.util.function.ToDoubleFunction;

public final class BalancedRouteStrategy extends ScoringRouteStrategy {
    private static final double WEIGHT_TOLERANCE = 1e-9;

    private final double timeWeight;
    private final double costWeight;
    private final double distanceWeight;
    private final double emissionWeight;

    public BalancedRouteStrategy(double timeWeight, double costWeight,
                                 double distanceWeight, double emissionWeight) {
        Validation.nonNegative(timeWeight, "Time weight");
        Validation.nonNegative(costWeight, "Cost weight");
        Validation.nonNegative(distanceWeight, "Distance weight");
        Validation.nonNegative(emissionWeight, "Emission weight");
        double sum = timeWeight + costWeight + distanceWeight + emissionWeight;
        if (timeWeight > 1 || costWeight > 1 || distanceWeight > 1 || emissionWeight > 1
                || Math.abs(sum - 1.0) > WEIGHT_TOLERANCE) {
            throw new IllegalArgumentException("Balanced weights must be between 0 and 1 and sum to 1");
        }
        this.timeWeight = timeWeight;
        this.costWeight = costWeight;
        this.distanceWeight = distanceWeight;
        this.emissionWeight = emissionWeight;
    }

    @Override
    public String getStrategyName() {
        return "Balanced";
    }

    @Override
    protected ToDoubleFunction<Candidate> scoringFunction(List<Candidate> candidates) {
        Range time = Range.of(candidates, c -> c.metrics().deliveryMinutes());
        Range cost = Range.of(candidates, c -> c.metrics().totalCost());
        Range distance = Range.of(candidates, c -> c.route().distanceKm());
        Range emission = Range.of(candidates, c -> c.route().carbonEmission());

        return candidate -> time.normalize(candidate.metrics().deliveryMinutes()) * timeWeight
                + cost.normalize(candidate.metrics().totalCost()) * costWeight
                + distance.normalize(candidate.route().distanceKm()) * distanceWeight
                + emission.normalize(candidate.route().carbonEmission()) * emissionWeight;
    }

    @Override
    protected List<String> explain(Candidate selected, DeliveryRequest request) {
        return List.of(
                String.format(Locale.ROOT,
                        "Min-max normalized time * %.2f + cost * %.2f + distance * %.2f + CO2 * %.2f",
                        timeWeight, costWeight, distanceWeight, emissionWeight),
                "Ranges use feasible routes only; an equal-valued criterion contributes zero");
    }

    private record Range(double min, double max) {
        static Range of(List<Candidate> candidates, ToDoubleFunction<Candidate> metric) {
            var summary = candidates.stream().mapToDouble(metric).summaryStatistics();
            return new Range(summary.getMin(), summary.getMax());
        }

        double normalize(double value) {
            return max == min ? 0 : (value - min) / (max - min);
        }
    }
}

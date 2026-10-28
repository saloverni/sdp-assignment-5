package kz.delivery.model;

import java.util.Set;

public record DeliveryRequest(
        Location origin,
        Location destination,
        DeliveryPriority priority,
        double maxBudget,
        int maxDeliveryMinutes,
        boolean ecoPreferred,
        double maxCarbonEmission,
        Set<String> prohibitedRouteIds
) {
    public DeliveryRequest {
        Validation.required(origin, "Origin");
        Validation.required(destination, "Destination");
        Validation.required(priority, "Priority");
        Validation.nonNegative(maxBudget, "Budget");
        Validation.nonNegative(maxDeliveryMinutes, "Deadline");
        Validation.nonNegative(maxCarbonEmission, "Emission limit");
        Validation.required(prohibitedRouteIds, "Prohibited routes");
        prohibitedRouteIds.forEach(id -> Validation.text(id, "Prohibited route id"));
        prohibitedRouteIds = Set.copyOf(prohibitedRouteIds);
    }
}

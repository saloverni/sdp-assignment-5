package kz.delivery.model;

public record Route(
        String id,
        Location origin,
        Location destination,
        double distanceKm,
        int estimatedMinutes,
        double cost,
        double carbonEmission,
        int trafficDelayMinutes,
        int weatherDelayMinutes,
        double additionalFees,
        boolean available
) {
    public Route {
        Validation.text(id, "Route id");
        Validation.required(origin, "Origin");
        Validation.required(destination, "Destination");
        Validation.nonNegative(distanceKm, "Distance");
        Validation.nonNegative(estimatedMinutes, "Estimated time");
        Validation.nonNegative(cost, "Base cost");
        Validation.nonNegative(carbonEmission, "Carbon emission");
        Validation.nonNegative(trafficDelayMinutes, "Traffic delay");
        Validation.nonNegative(weatherDelayMinutes, "Weather delay");
        Validation.nonNegative(additionalFees, "Additional fees");
    }
}

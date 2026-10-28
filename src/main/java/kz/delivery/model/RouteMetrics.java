package kz.delivery.model;

public record RouteMetrics(double deliveryMinutes, double totalCost) {
    public static final double COST_PER_KM = 75;
    public static final double URGENT_SURCHARGE = 800;

    public RouteMetrics {
        Validation.nonNegative(deliveryMinutes, "Delivery time");
        Validation.nonNegative(totalCost, "Total cost");
    }

    public static RouteMetrics calculate(Route route, DeliveryRequest request) {
        double time = (double) route.estimatedMinutes()
                + route.trafficDelayMinutes() + route.weatherDelayMinutes();
        double surcharge = request.priority() == DeliveryPriority.URGENT ? URGENT_SURCHARGE : 0;
        double cost = route.cost() + route.distanceKm() * COST_PER_KM
                + route.additionalFees() + surcharge;
        return new RouteMetrics(time, cost);
    }
}

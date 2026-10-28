package kz.delivery.service;

import kz.delivery.model.DeliveryPriority;
import kz.delivery.model.DeliveryRequest;
import kz.delivery.model.Validation;
import kz.delivery.strategy.RouteStrategy;

import java.util.EnumMap;
import java.util.Map;

public final class StrategySelector {
    private final Map<DeliveryPriority, RouteStrategy> strategies = new EnumMap<>(DeliveryPriority.class);

    public StrategySelector(Map<DeliveryPriority, RouteStrategy> strategies) {
        Validation.required(strategies, "Strategy mappings");
        strategies.forEach(this::register);
    }

    public void register(DeliveryPriority priority, RouteStrategy strategy) {
        strategies.put(Validation.required(priority, "Priority"), Validation.required(strategy, "Strategy"));
    }

    public RouteStrategy select(DeliveryRequest request) {
        Validation.required(request, "Request");
        DeliveryPriority priority = request.priority();
        if (priority != DeliveryPriority.URGENT && request.ecoPreferred()) {
            priority = DeliveryPriority.ECO;
        }
        RouteStrategy strategy = strategies.get(priority);
        if (strategy == null) {
            throw new IllegalStateException("No strategy registered for " + priority);
        }
        return strategy;
    }
}

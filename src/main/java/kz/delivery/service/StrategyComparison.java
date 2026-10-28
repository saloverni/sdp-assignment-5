package kz.delivery.service;

import kz.delivery.model.DeliveryRequest;
import kz.delivery.model.Route;
import kz.delivery.model.RouteDecision;
import kz.delivery.model.Validation;
import kz.delivery.strategy.RouteStrategy;

import java.util.List;

public final class StrategyComparison {
    private final List<RouteStrategy> strategies;

    public StrategyComparison(List<RouteStrategy> strategies) {
        Validation.required(strategies, "Strategies");
        if (strategies.isEmpty()) {
            throw new IllegalArgumentException("At least one strategy is required");
        }
        strategies.forEach(strategy -> Validation.required(strategy, "Strategy"));
        this.strategies = List.copyOf(strategies);
    }

    public List<RouteDecision> compare(List<Route> routes, DeliveryRequest request) {
        return strategies.stream().map(strategy -> new RoutePlanner(strategy).plan(routes, request)).toList();
    }
}

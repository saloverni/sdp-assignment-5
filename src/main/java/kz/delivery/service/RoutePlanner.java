package kz.delivery.service;

import kz.delivery.model.DeliveryRequest;
import kz.delivery.model.Route;
import kz.delivery.model.RouteDecision;
import kz.delivery.model.Validation;
import kz.delivery.strategy.RouteStrategy;

import java.util.List;

public final class RoutePlanner {
    private RouteStrategy strategy;

    public RoutePlanner(RouteStrategy strategy) {
        setStrategy(strategy);
    }

    public void setStrategy(RouteStrategy strategy) {
        this.strategy = Validation.required(strategy, "Strategy");
    }

    public RouteDecision plan(List<Route> routes, DeliveryRequest request) {
        return strategy.selectRoute(routes, request);
    }
}

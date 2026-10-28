package kz.delivery.strategy;

import kz.delivery.model.DeliveryRequest;
import kz.delivery.model.Route;
import kz.delivery.model.RouteDecision;

import java.util.List;

public interface RouteStrategy {
    RouteDecision selectRoute(List<Route> routes, DeliveryRequest request);

    String getStrategyName();
}

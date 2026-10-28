package kz.delivery.service;

import kz.delivery.exception.NoSuitableRouteException;
import kz.delivery.model.DeliveryRequest;
import kz.delivery.model.Route;
import kz.delivery.model.RouteMetrics;
import kz.delivery.model.Validation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RouteConstraints {
    private RouteConstraints() {}

    public record Candidate(Route route, RouteMetrics metrics) {}

    public record Evaluation(List<Candidate> candidates, Map<String, List<String>> rejectedRoutes) {}

    public static Evaluation evaluate(List<Route> routes, DeliveryRequest request) {
        Validation.required(routes, "Routes");
        Validation.required(request, "Request");
        List<Candidate> candidates = new ArrayList<>();
        Map<String, List<String>> rejected = new LinkedHashMap<>();
        Set<String> ids = new HashSet<>();

        for (Route route : routes) {
            Validation.required(route, "Route");
            if (!ids.add(route.id())) {
                throw new IllegalArgumentException("Duplicate route id: " + route.id());
            }
            RouteMetrics metrics = RouteMetrics.calculate(route, request);
            List<String> reasons = new ArrayList<>();
            if (!route.available()) {
                reasons.add("Route is unavailable");
            }
            if (!route.origin().equals(request.origin()) || !route.destination().equals(request.destination())) {
                reasons.add("Origin or destination does not match the request");
            }
            if (request.prohibitedRouteIds().contains(route.id())) {
                reasons.add("Route is prohibited for this delivery");
            }
            if (metrics.totalCost() > request.maxBudget()) {
                reasons.add("Budget exceeded");
            }
            if (metrics.deliveryMinutes() > request.maxDeliveryMinutes()) {
                reasons.add("Delivery deadline exceeded");
            }
            if (route.carbonEmission() > request.maxCarbonEmission()) {
                reasons.add("Emission limit exceeded");
            }
            if (reasons.isEmpty()) {
                candidates.add(new Candidate(route, metrics));
            } else {
                rejected.put(route.id(), List.copyOf(reasons));
            }
        }
        if (candidates.isEmpty()) {
            throw new NoSuitableRouteException(routes.isEmpty()
                    ? "No candidate routes were provided"
                    : "No route satisfies the delivery constraints", rejected);
        }
        return new Evaluation(List.copyOf(candidates), rejected);
    }
}

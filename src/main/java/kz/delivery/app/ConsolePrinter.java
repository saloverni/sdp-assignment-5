package kz.delivery.app;

import kz.delivery.model.DeliveryRequest;
import kz.delivery.model.Route;
import kz.delivery.model.RouteDecision;
import kz.delivery.model.RouteMetrics;

import java.util.List;
import java.util.Locale;

public final class ConsolePrinter {
    private ConsolePrinter() {}

    public static void routes(List<Route> routes, DeliveryRequest request) {
        System.out.println("CANDIDATE ROUTES (standard delivery prices)");
        System.out.println("ID  Distance  Time    Cost (KZT)  CO2 (kg)  Available");
        for (Route route : routes) {
            RouteMetrics metrics = RouteMetrics.calculate(route, request);
            System.out.printf(Locale.ROOT, "%s  %5.1f km  %3.0f min  %8.2f    %5.2f    %s%n",
                    route.id(), route.distanceKm(), metrics.deliveryMinutes(), metrics.totalCost(),
                    route.carbonEmission(), route.available());
        }
    }

    public static void request(DeliveryRequest request) {
        System.out.printf(Locale.ROOT,
                "%nRequest: %s | budget %.2f KZT | deadline %d min | CO2 limit %.2f kg | eco preferred %s%n",
                request.priority(), request.maxBudget(), request.maxDeliveryMinutes(),
                request.maxCarbonEmission(), request.ecoPreferred());
    }

    public static void decision(RouteDecision decision) {
        System.out.printf(Locale.ROOT, "%s -> %s | time %.0f min | cost %.2f KZT | distance %.1f km"
                        + " | CO2 %.2f kg | score %.4f%n",
                decision.strategyName(), decision.route().id(), decision.metrics().deliveryMinutes(),
                decision.metrics().totalCost(), decision.route().distanceKm(),
                decision.route().carbonEmission(), decision.score());
        decision.explanation().forEach(reason -> System.out.println("  " + reason));
        decision.candidateScores().forEach((id, score) ->
                System.out.printf(Locale.ROOT, "  Candidate %s: %.4f%n", id, score));
        decision.rejectedRoutes().forEach((id, reasons) ->
                System.out.println("  Rejected " + id + ": " + String.join("; ", reasons)));
    }
}

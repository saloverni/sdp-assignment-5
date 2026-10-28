package kz.delivery.app;

import kz.delivery.exception.NoSuitableRouteException;
import kz.delivery.model.DeliveryPriority;
import kz.delivery.model.DeliveryRequest;
import kz.delivery.model.Route;
import kz.delivery.service.RoutePlanner;
import kz.delivery.service.StrategyComparison;
import kz.delivery.service.StrategySelector;
import kz.delivery.strategy.BalancedRouteStrategy;
import kz.delivery.strategy.CheapestRouteStrategy;
import kz.delivery.strategy.DistanceFirstStrategy;
import kz.delivery.strategy.EcoFriendlyRouteStrategy;
import kz.delivery.strategy.FastestRouteStrategy;
import kz.delivery.strategy.RouteStrategy;

import java.util.List;
import java.util.Map;

public final class Main {
    private final List<Route> routes = DemoData.routes();
    private final RouteStrategy fastest = new FastestRouteStrategy();
    private final RouteStrategy cheapest = new CheapestRouteStrategy();
    private final RouteStrategy eco = new EcoFriendlyRouteStrategy();
    private final RouteStrategy balanced = new BalancedRouteStrategy(0.4, 0.3, 0.1, 0.2);
    private final List<RouteStrategy> strategies = List.of(fastest, cheapest, eco, balanced, new DistanceFirstStrategy());
    private final StrategySelector selector = new StrategySelector(Map.of(
            DeliveryPriority.URGENT, fastest,
            DeliveryPriority.ECONOMY, cheapest,
            DeliveryPriority.ECO, eco,
            DeliveryPriority.STANDARD, balanced));

    public static void main(String[] args) {
        String mode = args.length == 0 ? "--demo" : args[0];
        if (args.length > 1 || !List.of("--demo", "--compare", "--switch", "--auto", "--failures", "--help").contains(mode)) {
            System.err.println("Usage: route-planner [--demo|--compare|--switch|--auto|--failures|--help]");
            System.exit(2);
        }
        Main app = new Main();
        switch (mode) {
            case "--compare" -> app.compare();
            case "--switch" -> app.switchStrategies();
            case "--auto" -> app.automaticSelection();
            case "--failures" -> app.failureScenarios();
            case "--help" -> System.out.println("Modes: --demo (default), --compare, --switch, --auto, --failures");
            default -> {
                ConsolePrinter.routes(app.routes, DemoData.standardRequest());
                app.switchStrategies();
                app.automaticSelection();
                app.compare();
                app.failureScenarios();
            }
        }
    }

    private void switchStrategies() {
        System.out.println("\nRUNTIME SWITCHING (one planner, three switches)");
        DeliveryRequest request = DemoData.standardRequest();
        ConsolePrinter.request(request);
        RoutePlanner planner = new RoutePlanner(fastest);
        ConsolePrinter.decision(planner.plan(routes, request));
        for (RouteStrategy strategy : List.of(cheapest, eco, balanced)) {
            planner.setStrategy(strategy);
            ConsolePrinter.decision(planner.plan(routes, request));
        }
    }

    private void automaticSelection() {
        System.out.println("\nAUTOMATIC SELECTION");
        List<DeliveryRequest> requests = List.of(
                DemoData.request(DeliveryPriority.URGENT, 10000, 30, false, 10),
                DemoData.request(DeliveryPriority.ECONOMY, 2500, 60, false, 10),
                DemoData.request(DeliveryPriority.ECO, 4000, 50, true, 2),
                DemoData.standardRequest(),
                DemoData.request(DeliveryPriority.STANDARD, 4000, 50, true, 10));
        for (DeliveryRequest request : requests) {
            ConsolePrinter.request(request);
            RoutePlanner planner = new RoutePlanner(selector.select(request));
            ConsolePrinter.decision(planner.plan(routes, request));
        }
    }

    private void compare() {
        System.out.println("\nSTRATEGY COMPARISON");
        DeliveryRequest request = DemoData.standardRequest();
        ConsolePrinter.request(request);
        new StrategyComparison(strategies).compare(routes, request).forEach(ConsolePrinter::decision);
    }

    private void failureScenarios() {
        System.out.println("\nFAILURE SCENARIOS");
        showFailure("Empty route list", () -> new RoutePlanner(fastest).plan(List.of(), DemoData.standardRequest()));
        showFailure("No route within budget", () -> new RoutePlanner(cheapest).plan(routes,
                DemoData.request(DeliveryPriority.ECONOMY, 100, 60, false, 10)));
        showFailure("Invalid balanced weights", () -> new BalancedRouteStrategy(0.5, 0.5, 0.5, 0.5));
        showFailure("Missing request", () -> new RoutePlanner(fastest).plan(routes, null));
    }

    private void showFailure(String label, Runnable action) {
        try {
            action.run();
            throw new IllegalStateException("Expected scenario to fail: " + label);
        } catch (NoSuitableRouteException exception) {
            System.out.println(label + ": " + exception.getMessage());
            exception.getRejectedRoutes().forEach((id, reasons) ->
                    System.out.println("  " + id + ": " + String.join("; ", reasons)));
        } catch (IllegalArgumentException exception) {
            System.out.println(label + ": " + exception.getMessage());
        }
    }
}

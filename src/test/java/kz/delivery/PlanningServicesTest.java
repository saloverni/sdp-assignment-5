package kz.delivery;

import kz.delivery.app.DemoData;
import kz.delivery.model.DeliveryPriority;
import kz.delivery.model.Route;
import kz.delivery.model.RouteDecision;
import kz.delivery.service.RoutePlanner;
import kz.delivery.service.StrategyComparison;
import kz.delivery.service.StrategySelector;
import kz.delivery.strategy.BalancedRouteStrategy;
import kz.delivery.strategy.CheapestRouteStrategy;
import kz.delivery.strategy.DistanceFirstStrategy;
import kz.delivery.strategy.EcoFriendlyRouteStrategy;
import kz.delivery.strategy.FastestRouteStrategy;
import kz.delivery.strategy.RouteStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static kz.delivery.TestData.*;
import static org.junit.jupiter.api.Assertions.*;

class PlanningServicesTest {
    private final RouteStrategy fastest = new FastestRouteStrategy();
    private final RouteStrategy cheapest = new CheapestRouteStrategy();
    private final RouteStrategy eco = new EcoFriendlyRouteStrategy();
    private final RouteStrategy balanced = new BalancedRouteStrategy(0.4, 0.3, 0.1, 0.2);
    private final Map<DeliveryPriority, RouteStrategy> mappings = Map.of(
            DeliveryPriority.URGENT, fastest, DeliveryPriority.ECONOMY, cheapest,
            DeliveryPriority.ECO, eco, DeliveryPriority.STANDARD, balanced);

    @Test
    void onePlannerSupportsThreeRuntimeSwitches() {
        var planner = new RoutePlanner(fastest);
        var routes = DemoData.routes();
        var request = DemoData.standardRequest();
        assertEquals("R2", planner.plan(routes, request).route().id());
        planner.setStrategy(cheapest);
        assertEquals("R3", planner.plan(routes, request).route().id());
        planner.setStrategy(eco);
        assertEquals("R4", planner.plan(routes, request).route().id());
        planner.setStrategy(balanced);
        assertEquals("Balanced", planner.plan(routes, request).strategyName());
        assertEquals(0.3284782609, planner.plan(routes, request).score(), 1e-9);
    }

    @ParameterizedTest
    @EnumSource(DeliveryPriority.class)
    void selectorUsesRegisteredMappingForEachPriority(DeliveryPriority priority) {
        var selector = new StrategySelector(mappings);
        var request = request(priority, 10000, 120, false, 100, Set.of());
        assertSame(mappings.get(priority), selector.select(request));
    }

    @Test
    void ecoPreferenceOverridesEconomyButDoesNotOverrideUrgency() {
        var selector = new StrategySelector(mappings);
        assertSame(eco, selector.select(request(DeliveryPriority.ECONOMY, 10000, 120, true, 100, Set.of())));
        assertSame(fastest, selector.select(request(DeliveryPriority.URGENT, 10000, 120, true, 100, Set.of())));
    }

    @Test
    void strategyRegistrationCanBeReplacedWithoutChangingThePlanner() {
        var selector = new StrategySelector(mappings);
        var shortest = new DistanceFirstStrategy();
        selector.register(DeliveryPriority.STANDARD, shortest);
        var planner = new RoutePlanner(selector.select(DemoData.standardRequest()));
        assertEquals("R5", planner.plan(DemoData.routes(), DemoData.standardRequest()).route().id());
    }

    @Test
    void missingRegistrationAndNullStrategyFailExplicitly() {
        var selector = new StrategySelector(Map.of());
        assertThrows(IllegalStateException.class, () -> selector.select(request()));
        assertThrows(IllegalArgumentException.class, () -> new RoutePlanner(null));
        var planner = new RoutePlanner(fastest);
        assertThrows(IllegalArgumentException.class, () -> planner.setStrategy(null));
        assertEquals("R2", planner.plan(DemoData.routes(), DemoData.standardRequest()).route().id());
        assertThrows(IllegalArgumentException.class, () -> selector.register(DeliveryPriority.ECO, null));
    }

    @Test
    void comparisonRunsAllStrategiesAgainstTheSameInputWithoutMutatingRoutes() {
        List<Route> routes = new ArrayList<>(DemoData.routes());
        List<Route> before = List.copyOf(routes);
        var comparison = new StrategyComparison(List.of(fastest, cheapest, eco, balanced, new DistanceFirstStrategy()));
        List<RouteDecision> results = comparison.compare(routes, DemoData.standardRequest());
        assertEquals(List.of("R2", "R3", "R4", "R4", "R5"),
                results.stream().map(result -> result.route().id()).toList());
        assertEquals(before, routes);
        assertTrue(results.stream().allMatch(result -> result.rejectedRoutes().containsKey("R7")));
    }

    @Test
    void automaticRequestsProduceDifferentRoutes() {
        var selector = new StrategySelector(mappings);
        var urgent = DemoData.request(DeliveryPriority.URGENT, 10000, 30, false, 10);
        var economy = DemoData.request(DeliveryPriority.ECONOMY, 2500, 60, false, 10);
        assertEquals("R8", new RoutePlanner(selector.select(urgent)).plan(DemoData.routes(), urgent).route().id());
        assertEquals("R3", new RoutePlanner(selector.select(economy)).plan(DemoData.routes(), economy).route().id());
    }

    @Test
    void comparisonRequiresAtLeastOneStrategy() {
        assertThrows(IllegalArgumentException.class, () -> new StrategyComparison(List.of()));
    }
}

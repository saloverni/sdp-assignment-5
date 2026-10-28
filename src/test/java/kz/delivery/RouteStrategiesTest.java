package kz.delivery;

import kz.delivery.model.DeliveryPriority;
import kz.delivery.model.Route;
import kz.delivery.model.RouteDecision;
import kz.delivery.strategy.BalancedRouteStrategy;
import kz.delivery.strategy.CheapestRouteStrategy;
import kz.delivery.strategy.DistanceFirstStrategy;
import kz.delivery.strategy.EcoFriendlyRouteStrategy;
import kz.delivery.strategy.FastestRouteStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static kz.delivery.TestData.*;
import static org.junit.jupiter.api.Assertions.*;

class RouteStrategiesTest {
    @Test
    void fastestIncludesTrafficAndWeatherInsteadOfUsingBaseTime() {
        Route delayed = new Route("A", ORIGIN, DESTINATION, 10, 10, 500, 2, 8, 9, 0, true);
        Route clear = route("B", 10, 25, 500, 2);
        RouteDecision decision = new FastestRouteStrategy().selectRoute(List.of(delayed, clear), request());
        assertEquals(clear, decision.route());
        assertEquals(25, decision.score());
        assertEquals(27, decision.candidateScores().get("A"));
        assertTrue(decision.explanation().get(0).contains("25 base + 0 traffic + 0 weather"));
    }

    @Test
    void cheapestIncludesDistanceAndFeesInsteadOfOnlyBaseCost() {
        Route longRoute = new Route("A", ORIGIN, DESTINATION, 30, 20, 100, 2, 0, 0, 300, true);
        Route shortRoute = new Route("B", ORIGIN, DESTINATION, 5, 20, 500, 2, 0, 0, 100, true);
        RouteDecision decision = new CheapestRouteStrategy().selectRoute(List.of(longRoute, shortRoute), request());
        assertEquals(shortRoute, decision.route());
        assertEquals(975, decision.metrics().totalCost());
        assertEquals(2650, decision.candidateScores().get("A"));
    }

    @Test
    void urgentDeliveryAddsPrioritySurcharge() {
        Route route = route("A", 10, 20, 500, 2);
        var urgent = request(DeliveryPriority.URGENT, 10000, 120, false, 100, Set.of());
        RouteDecision decision = new CheapestRouteStrategy().selectRoute(List.of(route), urgent);
        assertEquals(2050, decision.metrics().totalCost());
        assertTrue(decision.explanation().get(0).contains("800.00 priority"));
    }

    @Test
    void ecoScoreIncludesBothEmissionAndDistance() {
        Route lowEmission = route("A", 30, 20, 500, 0.1);
        Route shortRoute = route("B", 5, 20, 500, 2);
        RouteDecision decision = new EcoFriendlyRouteStrategy().selectRoute(List.of(lowEmission, shortRoute), request());
        assertEquals(shortRoute, decision.route());
        assertEquals(2.6, decision.score(), 1e-9);
        assertEquals(6.08, decision.candidateScores().get("A"), 1e-9);
    }

    @Test
    void balancedNormalizesDifferentUnitsBeforeWeighting() {
        List<Route> routes = balancedRoutes();
        RouteDecision decision = new BalancedRouteStrategy(0.4, 0.3, 0.1, 0.2).selectRoute(routes, request());
        assertEquals("B", decision.route().id());
        assertEquals(0.4, decision.score(), 1e-9);
        assertEquals(0.6, decision.candidateScores().get("A"), 1e-9);
        assertEquals(0.5, decision.candidateScores().get("C"), 1e-9);
    }

    @Test
    void changingBalancedWeightsChangesTheWinner() {
        List<Route> routes = balancedRoutes();
        assertEquals("A", new BalancedRouteStrategy(1, 0, 0, 0).selectRoute(routes, request()).route().id());
        assertEquals("B", new BalancedRouteStrategy(0, 1, 0, 0).selectRoute(routes, request()).route().id());
    }

    @Test
    void balancedHandlesEqualMetricsAndBreaksTiesById() {
        List<Route> routes = List.of(route("B", 10, 20, 500, 2), route("A", 10, 20, 500, 2));
        RouteDecision decision = new BalancedRouteStrategy(0.4, 0.3, 0.1, 0.2).selectRoute(routes, request());
        assertEquals("A", decision.route().id());
        assertEquals(0, decision.score());
    }

    @Test
    void balancedHandlesOneFeasibleRoute() {
        Route route = route("A", 10, 20, 500, 2);
        RouteDecision decision = new BalancedRouteStrategy(0.4, 0.3, 0.1, 0.2).selectRoute(List.of(route), request());
        assertEquals(route, decision.route());
        assertEquals(0, decision.score());
    }

    @Test
    void rejectedOutlierDoesNotChangeBalancedNormalization() {
        List<Route> routes = new ArrayList<>(balancedRoutes());
        var strategy = new BalancedRouteStrategy(0.4, 0.3, 0.1, 0.2);
        var before = strategy.selectRoute(routes, request());
        routes.add(route("Outlier", 1000, 1000, 50000, 1000));
        var after = strategy.selectRoute(routes, request());
        assertEquals(before.candidateScores(), after.candidateScores());
        assertEquals(before.route(), after.route());
        assertTrue(after.rejectedRoutes().containsKey("Outlier"));
    }

    @Test
    void balancedRejectsWeightsWithIncorrectSum() {
        assertThrows(IllegalArgumentException.class, () -> new BalancedRouteStrategy(0.4, 0.4, 0.4, 0.4));
        assertThrows(IllegalArgumentException.class, () -> new BalancedRouteStrategy(0, 0, 0, 0));
    }

    @ParameterizedTest
    @ValueSource(doubles = {-0.1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void balancedRejectsNegativeOrNonFiniteWeights(double weight) {
        assertThrows(IllegalArgumentException.class, () -> new BalancedRouteStrategy(weight, 0.3, 0.1, 0.2));
    }

    @Test
    void fifthStrategyMinimizesDistanceRatherThanTime() {
        Route fast = route("A", 20, 10, 500, 2);
        Route shortRoute = route("B", 5, 30, 500, 2);
        RouteDecision decision = new DistanceFirstStrategy().selectRoute(List.of(fast, shortRoute), request());
        assertEquals(shortRoute, decision.route());
        assertEquals(5, decision.score());
    }

    private List<Route> balancedRoutes() {
        return List.of(route("A", 30, 10, 750, 4),
                route("B", 10, 30, 250, 0), route("C", 20, 20, 500, 2));
    }
}

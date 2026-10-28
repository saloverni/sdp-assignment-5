package kz.delivery;

import kz.delivery.exception.NoSuitableRouteException;
import kz.delivery.model.DeliveryPriority;
import kz.delivery.model.Location;
import kz.delivery.model.Route;
import kz.delivery.strategy.BalancedRouteStrategy;
import kz.delivery.strategy.CheapestRouteStrategy;
import kz.delivery.strategy.DistanceFirstStrategy;
import kz.delivery.strategy.EcoFriendlyRouteStrategy;
import kz.delivery.strategy.FastestRouteStrategy;
import kz.delivery.strategy.RouteStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static kz.delivery.TestData.*;
import static org.junit.jupiter.api.Assertions.*;

class RouteConstraintsTest {
    static Stream<RouteStrategy> strategies() {
        return Stream.of(new FastestRouteStrategy(), new CheapestRouteStrategy(),
                new EcoFriendlyRouteStrategy(), new BalancedRouteStrategy(0.4, 0.3, 0.1, 0.2),
                new DistanceFirstStrategy());
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void everyStrategyRejectsUnavailableRoutesEvenWithBetterScores(RouteStrategy strategy) {
        Route closed = new Route("Closed", ORIGIN, DESTINATION, 0, 0, 0, 0, 0, 0, 0, false);
        Route open = route("Open", 10, 20, 500, 2);
        var decision = strategy.selectRoute(List.of(closed, open), request());
        assertEquals(open, decision.route());
        assertEquals(List.of("Route is unavailable"), decision.rejectedRoutes().get("Closed"));
        assertFalse(decision.candidateScores().containsKey("Closed"));
    }

    @Test
    void budgetUsesTotalCostAndCanChangeTheFastestChoice() {
        Route fast = route("Fast", 20, 10, 500, 2);
        Route affordable = route("Affordable", 10, 30, 500, 2);
        var strategy = new FastestRouteStrategy();
        assertEquals(fast, strategy.selectRoute(List.of(fast, affordable), request()).route());
        var tightBudget = request(DeliveryPriority.STANDARD, 1500, 120, false, 100, Set.of());
        var decision = strategy.selectRoute(List.of(fast, affordable), tightBudget);
        assertEquals(affordable, decision.route());
        assertEquals(List.of("Budget exceeded"), decision.rejectedRoutes().get("Fast"));
    }

    @Test
    void deadlineIncludesTrafficAndWeatherDelays() {
        Route delayed = new Route("Delayed", ORIGIN, DESTINATION, 5, 10, 0, 2, 8, 8, 0, true);
        Route onTime = route("OnTime", 10, 20, 500, 2);
        var request = request(DeliveryPriority.STANDARD, 10000, 25, false, 100, Set.of());
        var decision = new CheapestRouteStrategy().selectRoute(List.of(delayed, onTime), request);
        assertEquals(onTime, decision.route());
        assertEquals(List.of("Delivery deadline exceeded"), decision.rejectedRoutes().get("Delayed"));
    }

    @Test
    void emissionLimitIsAHardConstraintForFastestStrategy() {
        Route dirty = route("Dirty", 10, 10, 500, 5);
        Route clean = route("Clean", 10, 20, 500, 1);
        var request = request(DeliveryPriority.STANDARD, 10000, 120, false, 2, Set.of());
        var decision = new FastestRouteStrategy().selectRoute(List.of(dirty, clean), request);
        assertEquals(clean, decision.route());
        assertEquals(List.of("Emission limit exceeded"), decision.rejectedRoutes().get("Dirty"));
    }

    @Test
    void prohibitedRoutesAndWrongDestinationsAreRejected() {
        Route prohibited = route("Prohibited", 1, 1, 0, 0);
        Route wrongDestination = new Route("Wrong", ORIGIN, new Location("Elsewhere", 44, 77),
                1, 1, 0, 0, 0, 0, 0, true);
        Route valid = route("Valid", 10, 20, 500, 2);
        var request = request(DeliveryPriority.STANDARD, 10000, 120, false, 100, Set.of("Prohibited"));
        var decision = new DistanceFirstStrategy().selectRoute(List.of(prohibited, wrongDestination, valid), request);
        assertEquals(valid, decision.route());
        assertTrue(decision.rejectedRoutes().get("Prohibited").contains("Route is prohibited for this delivery"));
        assertTrue(decision.rejectedRoutes().get("Wrong").contains("Origin or destination does not match the request"));
    }

    @Test
    void exactBudgetDeadlineAndEmissionLimitsAreAllowed() {
        Route route = route("Boundary", 10, 20, 500, 2);
        var request = request(DeliveryPriority.STANDARD, 1250, 20, false, 2, Set.of());
        assertEquals(route, new FastestRouteStrategy().selectRoute(List.of(route), request).route());
    }

    @Test
    void urgentSurchargeCanMakeARouteExceedBudget() {
        Route route = route("A", 10, 20, 500, 2);
        var request = request(DeliveryPriority.URGENT, 2000, 120, false, 100, Set.of());
        var error = assertThrows(NoSuitableRouteException.class,
                () -> new CheapestRouteStrategy().selectRoute(List.of(route), request));
        assertEquals(List.of("Budget exceeded"), error.getRejectedRoutes().get("A"));
    }

    @Test
    void noSuitableRouteReportsEveryViolation() {
        Route route = route("A", 10, 20, 500, 2);
        var request = request(DeliveryPriority.STANDARD, 1, 1, false, 1, Set.of());
        var error = assertThrows(NoSuitableRouteException.class,
                () -> new FastestRouteStrategy().selectRoute(List.of(route), request));
        assertEquals(List.of("Budget exceeded", "Delivery deadline exceeded", "Emission limit exceeded"),
                error.getRejectedRoutes().get("A"));
    }

    @Test
    void emptyListHasAnExplicitFailure() {
        var error = assertThrows(NoSuitableRouteException.class,
                () -> new FastestRouteStrategy().selectRoute(List.of(), request()));
        assertEquals("No candidate routes were provided", error.getMessage());
        assertTrue(error.getRejectedRoutes().isEmpty());
    }

    @Test
    void missingInputAndDuplicateIdsAreRejected() {
        var strategy = new FastestRouteStrategy();
        Route route = route("A", 10, 20, 500, 2);
        assertThrows(IllegalArgumentException.class, () -> strategy.selectRoute(null, request()));
        assertThrows(IllegalArgumentException.class, () -> strategy.selectRoute(List.of(route), null));
        assertThrows(IllegalArgumentException.class, () -> strategy.selectRoute(Arrays.asList(route, null), request()));
        var error = assertThrows(IllegalArgumentException.class,
                () -> strategy.selectRoute(List.of(route, route), request()));
        assertEquals("Duplicate route id: A", error.getMessage());
    }

    @Test
    void explanationsAreImmutableSnapshots() {
        Route route = route("A", 10, 20, 500, 2);
        Route rejected = route("B", 1000, 20, 500, 2);
        var decision = new FastestRouteStrategy().selectRoute(List.of(route, rejected), request());
        assertThrows(UnsupportedOperationException.class, () -> decision.explanation().add("changed"));
        assertThrows(UnsupportedOperationException.class, () -> decision.candidateScores().put("A", 99.0));
        assertThrows(UnsupportedOperationException.class, () -> decision.rejectedRoutes().get("B").clear());
    }
}

package kz.delivery;

import kz.delivery.model.DeliveryPriority;
import kz.delivery.model.DeliveryRequest;
import kz.delivery.model.Location;
import kz.delivery.model.Route;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.Set;

import static kz.delivery.TestData.*;
import static org.junit.jupiter.api.Assertions.*;

class DomainValidationTest {
    @ParameterizedTest
    @ValueSource(doubles = {-1, Double.NaN, Double.POSITIVE_INFINITY})
    void invalidDistanceCostAndEmissionAreRejected(double value) {
        assertThrows(IllegalArgumentException.class, () -> route("A", value, 20, 500, 2));
        assertThrows(IllegalArgumentException.class, () -> route("A", 10, 20, value, 2));
        assertThrows(IllegalArgumentException.class, () -> route("A", 10, 20, 500, value));
    }

    @Test
    void negativeTimesFeesAndMissingEndpointsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> route("A", 10, -1, 500, 2));
        assertThrows(IllegalArgumentException.class,
                () -> new Route("A", ORIGIN, DESTINATION, 10, 20, 500, 2, -1, 0, 0, true));
        assertThrows(IllegalArgumentException.class,
                () -> new Route("A", ORIGIN, DESTINATION, 10, 20, 500, 2, 0, -1, 0, true));
        assertThrows(IllegalArgumentException.class,
                () -> new Route("A", ORIGIN, DESTINATION, 10, 20, 500, 2, 0, 0, -1, true));
        assertThrows(IllegalArgumentException.class,
                () -> new Route("A", null, DESTINATION, 10, 20, 500, 2, 0, 0, 0, true));
    }

    @Test
    void requestRejectsMissingPriorityAndInvalidLimits() {
        assertThrows(IllegalArgumentException.class, () -> request(null, 1000, 60, false, 10, Set.of()));
        assertThrows(IllegalArgumentException.class,
                () -> request(DeliveryPriority.STANDARD, -1, 60, false, 10, Set.of()));
        assertThrows(IllegalArgumentException.class,
                () -> request(DeliveryPriority.STANDARD, 1000, -1, false, 10, Set.of()));
        assertThrows(IllegalArgumentException.class,
                () -> request(DeliveryPriority.STANDARD, 1000, 60, false, Double.NaN, Set.of()));
    }

    @Test
    void requestCopiesProhibitedRouteIds() {
        Set<String> prohibited = new HashSet<>(Set.of("A"));
        DeliveryRequest request = request(DeliveryPriority.STANDARD, 1000, 60, false, 10, prohibited);
        prohibited.add("B");
        assertEquals(Set.of("A"), request.prohibitedRouteIds());
        assertThrows(UnsupportedOperationException.class, () -> request.prohibitedRouteIds().add("C"));
    }

    @Test
    void locationsRejectInvalidCoordinatesAndBlankNames() {
        assertThrows(IllegalArgumentException.class, () -> new Location("Depot", 91, 0));
        assertThrows(IllegalArgumentException.class, () -> new Location("Depot", 0, -181));
        assertThrows(IllegalArgumentException.class, () -> new Location("Depot", Double.NaN, 0));
        assertThrows(IllegalArgumentException.class, () -> new Location(" ", 0, 0));
    }
}

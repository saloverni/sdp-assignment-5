package kz.delivery;

import kz.delivery.model.DeliveryPriority;
import kz.delivery.model.DeliveryRequest;
import kz.delivery.model.Location;
import kz.delivery.model.Route;

import java.util.Set;

final class TestData {
    static final Location ORIGIN = new Location("Depot", 43.2, 76.9);
    static final Location DESTINATION = new Location("Customer", 43.3, 77.0);

    private TestData() {}

    static Route route(String id, double distance, int minutes, double baseCost, double emission) {
        return new Route(id, ORIGIN, DESTINATION, distance, minutes, baseCost, emission, 0, 0, 0, true);
    }

    static DeliveryRequest request() {
        return request(DeliveryPriority.STANDARD, 10000, 120, false, 100, Set.of());
    }

    static DeliveryRequest request(DeliveryPriority priority, double budget, int deadline,
                                   boolean ecoPreferred, double emissions, Set<String> prohibited) {
        return new DeliveryRequest(ORIGIN, DESTINATION, priority, budget, deadline,
                ecoPreferred, emissions, prohibited);
    }
}

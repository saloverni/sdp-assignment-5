package kz.delivery.app;

import kz.delivery.model.DeliveryPriority;
import kz.delivery.model.DeliveryRequest;
import kz.delivery.model.Location;
import kz.delivery.model.Route;

import java.util.List;
import java.util.Set;

public final class DemoData {
    public static final Location DEPOT = new Location("Almaty depot", 43.2389, 76.8897);
    public static final Location CUSTOMER = new Location("Customer", 43.2567, 76.9286);

    private DemoData() {}

    public static List<Route> routes() {
        return List.of(
                new Route("R1", DEPOT, CUSTOMER, 18, 20, 2700, 5.8, 14, 2, 500, true),
                new Route("R2", DEPOT, CUSTOMER, 24, 18, 3000, 6.5, 3, 1, 600, true),
                new Route("R3", DEPOT, CUSTOMER, 20, 38, 900, 3.8, 5, 2, 0, true),
                new Route("R4", DEPOT, CUSTOMER, 16, 30, 1700, 0.7, 4, 2, 100, true),
                new Route("R5", DEPOT, CUSTOMER, 12, 24, 2100, 3.0, 8, 3, 150, true),
                new Route("R6", DEPOT, CUSTOMER, 28, 34, 1000, 4.8, 1, 1, 0, true),
                new Route("R7", DEPOT, CUSTOMER, 10, 10, 500, 0.4, 0, 0, 0, false),
                new Route("R8", DEPOT, CUSTOMER, 21, 19, 5000, 7.0, 1, 1, 1000, true)
        );
    }

    public static DeliveryRequest standardRequest() {
        return request(DeliveryPriority.STANDARD, 6000, 60, false, 10);
    }

    public static DeliveryRequest request(DeliveryPriority priority, double budget, int deadline,
                                          boolean ecoPreferred, double emissionLimit) {
        return new DeliveryRequest(DEPOT, CUSTOMER, priority, budget, deadline,
                ecoPreferred, emissionLimit, Set.of());
    }
}

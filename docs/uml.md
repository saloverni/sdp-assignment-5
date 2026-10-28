# UML diagrams

## Class diagram

```mermaid
classDiagram
    class RoutePlanner {
        -RouteStrategy strategy
        +RoutePlanner(RouteStrategy strategy)
        +setStrategy(RouteStrategy strategy) void
        +plan(List~Route~ routes, DeliveryRequest request) RouteDecision
    }
    class RouteStrategy {
        <<interface>>
        +selectRoute(List~Route~ routes, DeliveryRequest request) RouteDecision
        +getStrategyName() String
    }
    class ScoringRouteStrategy {
        <<abstract>>
        +selectRoute(List~Route~ routes, DeliveryRequest request) RouteDecision
        #scoringFunction(List~Candidate~ candidates) ToDoubleFunction
        #explain(Candidate selected, DeliveryRequest request) List~String~
    }
    class FastestRouteStrategy
    class CheapestRouteStrategy
    class EcoFriendlyRouteStrategy
    class BalancedRouteStrategy {
        -double timeWeight
        -double costWeight
        -double distanceWeight
        -double emissionWeight
    }
    class DistanceFirstStrategy
    class StrategySelector {
        -Map strategies
        +register(DeliveryPriority priority, RouteStrategy strategy) void
        +select(DeliveryRequest request) RouteStrategy
    }
    class StrategyComparison {
        -List~RouteStrategy~ strategies
        +compare(List~Route~ routes, DeliveryRequest request) List~RouteDecision~
    }
    class RouteConstraints {
        +evaluate(List~Route~ routes, DeliveryRequest request) Evaluation$
    }
    class Route {
        <<record>>
        +String id
        +Location origin
        +Location destination
        +double distanceKm
        +int estimatedMinutes
        +double cost
        +double carbonEmission
        +int trafficDelayMinutes
        +int weatherDelayMinutes
        +double additionalFees
        +boolean available
    }
    class DeliveryRequest {
        <<record>>
        +Location origin
        +Location destination
        +DeliveryPriority priority
        +double maxBudget
        +int maxDeliveryMinutes
        +boolean ecoPreferred
        +double maxCarbonEmission
        +Set~String~ prohibitedRouteIds
    }
    class RouteMetrics {
        <<record>>
        +double deliveryMinutes
        +double totalCost
        +calculate(Route route, DeliveryRequest request) RouteMetrics$
    }
    class RouteDecision {
        <<record>>
        +Route route
        +String strategyName
        +double score
        +RouteMetrics metrics
        +List~String~ explanation
        +Map candidateScores
        +Map rejectedRoutes
    }
    class Location {
        <<record>>
        +String name
        +double latitude
        +double longitude
    }
    class DeliveryPriority {
        <<enumeration>>
        URGENT
        ECONOMY
        ECO
        STANDARD
    }
    class NoSuitableRouteException

    RoutePlanner o-- RouteStrategy : current strategy
    RouteStrategy <|.. ScoringRouteStrategy
    ScoringRouteStrategy <|-- FastestRouteStrategy
    ScoringRouteStrategy <|-- CheapestRouteStrategy
    ScoringRouteStrategy <|-- EcoFriendlyRouteStrategy
    ScoringRouteStrategy <|-- BalancedRouteStrategy
    ScoringRouteStrategy <|-- DistanceFirstStrategy
    StrategySelector o-- RouteStrategy : registered strategies
    StrategySelector ..> DeliveryRequest
    StrategyComparison o-- RouteStrategy
    StrategyComparison ..> RoutePlanner
    RouteStrategy ..> Route
    RouteStrategy ..> DeliveryRequest
    RouteStrategy ..> RouteDecision
    ScoringRouteStrategy ..> RouteConstraints
    RouteConstraints ..> RouteMetrics
    RouteConstraints ..> NoSuitableRouteException : throws
    RouteMetrics ..> Route
    RouteMetrics ..> DeliveryRequest
    RouteDecision --> Route
    RouteDecision --> RouteMetrics
    Route --> Location : endpoints
    DeliveryRequest --> Location : endpoints
    DeliveryRequest --> DeliveryPriority
```

## Sequence diagram

Successful urgent request from `Main.automaticSelection()`. The `selectRoute()` implementation is inherited from `ScoringRouteStrategy`; its calls to `scoringFunction()` and `explain()` dispatch to `FastestRouteStrategy`.

```mermaid
sequenceDiagram
    actor Client
    participant Main
    participant Selector as StrategySelector
    participant Planner as RoutePlanner
    participant Fastest as FastestRouteStrategy
    participant Constraints as RouteConstraints
    participant Metrics as RouteMetrics
    participant Decision as RouteDecision
    participant Printer as ConsolePrinter

    Client->>Main: run --auto
    Main->>Selector: select(urgentRequest)
    Selector-->>Main: fastestStrategy
    Main->>Planner: new RoutePlanner(fastestStrategy)
    Main->>Planner: plan(routes, urgentRequest)
    Planner->>Fastest: selectRoute(routes, urgentRequest)
    Note over Fastest: Shared selection flow inherited from ScoringRouteStrategy
    Fastest->>Constraints: evaluate(routes, urgentRequest)
    loop Each candidate route
        Constraints->>Metrics: calculate(route, urgentRequest)
        Metrics-->>Constraints: final time and total cost
        Constraints->>Constraints: check hard constraints
    end
    Constraints-->>Fastest: feasible candidates and rejection reasons
    Fastest->>Fastest: scoringFunction(feasibleCandidates)
    Fastest->>Fastest: choose minimum time, then route ID
    Fastest->>Fastest: explain(selected, urgentRequest)
    Fastest->>Decision: construct immutable decision with route, score and explanation
    Decision-->>Fastest: decision
    Fastest-->>Planner: decision
    Planner-->>Main: decision
    Main->>Printer: decision(decision)
    Printer-->>Client: selected route, metrics and explanation
```

If no route is feasible, `RouteConstraints.evaluate()` throws `NoSuitableRouteException` before scoring. The failure demonstration catches it and prints the rejection reasons.

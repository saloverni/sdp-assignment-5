# Smart Delivery and Route Optimization Engine

Assignment 5: a Java console application that selects a delivery route using the Strategy pattern. The same request can be optimized for time, price, environmental impact, a weighted balance, or distance. Eight candidate routes demonstrate different trade-offs, hard constraints, and runtime algorithm changes.

## Build and run

Requires JDK 17 or newer. Gradle 9.2.1 is included through the wrapper; the first build downloads Gradle and the JUnit dependencies.

```bash
./gradlew clean build
./gradlew run
```

On Windows, use `gradlew.bat` instead of `./gradlew`.

```bash
./gradlew run --args="--compare"
./gradlew run --args="--switch"
./gradlew run --args="--auto"
./gradlew run --args="--failures"
./gradlew test
```

The default `--demo` mode runs every scenario. The packaged application can also run directly:

```bash
java -jar build/libs/route-planner-1.0.0.jar --compare
```

## Architecture

`RoutePlanner` is the **Context**. It stores a `RouteStrategy`, delegates `plan()` to it, and supports replacement through `setStrategy()`. It does not inspect concrete strategy types or contain route-scoring algorithms.

`RouteStrategy` is the **Strategy interface**. Its `selectRoute()` method returns a `RouteDecision` rather than a bare route, so callers receive the selected route, metrics, score, explanation, scores of feasible alternatives, and reasons for rejected routes together.

The five **Concrete Strategies** are `FastestRouteStrategy`, `CheapestRouteStrategy`, `EcoFriendlyRouteStrategy`, `BalancedRouteStrategy`, and `DistanceFirstStrategy`. Their shared `ScoringRouteStrategy` base class handles filtering, score comparison, deterministic ties, and decision construction. Each concrete class supplies its own scoring rule and explanation. Ties are resolved by lexicographic route ID, independently of input order.

`RouteConstraints` checks feasibility before scoring. `RouteMetrics` calculates delivery time and total price consistently for every strategy and every constraint. `StrategySelector` chooses the algorithm from request properties; `StrategyComparison` runs all registered algorithms against one request. These responsibilities are separate from route optimization.

The domain consists of immutable `Location`, `Route`, `DeliveryRequest`, `RouteMetrics`, and `RouteDecision` records, plus the `DeliveryPriority` enum. The code is organized under `src/main/java/kz/delivery` into `model`, `strategy`, `service`, `exception`, and `app` packages.

Both the [UML class diagram and sequence diagram](docs/uml.md) reflect these classes and their actual calls.

## Algorithms and formulas

The demonstration uses minutes, kilometres, KZT, and kilograms of CO2. Values are illustrative estimates, not live traffic or pricing data.

### Fastest

```text
finalTime = estimatedMinutes + trafficDelayMinutes + weatherDelayMinutes
```

The lowest final time wins. A route with a shorter base time can lose after traffic and weather delays are included. The deadline check uses the same final time.

### Cheapest

```text
totalCost = baseCost + distanceKm * 75 + additionalFees + prioritySurcharge
prioritySurcharge = 800 for URGENT, otherwise 0
```

The lowest total cost wins. `Route.cost` holds the base cost. The formula lives in `RouteMetrics` because budget constraints and other strategies also need the actual delivery price. `CheapestRouteStrategy` makes total cost the optimization objective and explains its components.

### Eco friendly

```text
ecoScore = carbonEmission * 0.8 + distanceKm * 0.2
```

The lowest score wins. This formula belongs to the eco strategy because the relative importance of emissions and distance is an environmental optimization policy, not a general planning responsibility. The score is a policy index, not a physical CO2 quantity. The request's emission limit is checked separately against actual estimated kilograms of CO2.

### Balanced

Each criterion is normalized over **feasible candidates only**:

```text
normalizedValue = (value - minimum) / (maximum - minimum)
```

If all feasible routes have the same value for a criterion, its normalized value is zero. This also handles a single feasible route without division by zero.

```text
score = normalizedTime * timeWeight
      + normalizedCost * costWeight
      + normalizedDistance * distanceWeight
      + normalizedEmission * emissionWeight
```

Default weights are `0.40`, `0.30`, `0.10`, and `0.20`. They are constructor arguments, so another configuration can be supplied at runtime. Every weight must be finite and between zero and one; their sum must equal one within `1e-9`. Invalid configurations throw `IllegalArgumentException`.

Normalization prevents a price measured in thousands of KZT from overwhelming a time measured in tens of minutes. An infeasible outlier cannot affect the ranges.

### Distance first

The fifth strategy minimizes `distanceKm` after the same feasibility checks. It was added after the four-strategy application worked, in the separate commit `Add distance-first strategy without changing the planner`. Only one existing class changed: `Main`, to include the new strategy in comparison mode. The planner, selector, and other strategies required no changes.

## Request handling

The selector uses a configurable map:

| Request priority | Strategy |
| --- | --- |
| URGENT | Fastest |
| ECONOMY | Cheapest |
| ECO | Eco friendly |
| STANDARD | Balanced |

For non-urgent requests, `ecoPreferred=true` selects the ECO mapping. Urgency takes precedence over this preference. `register()` can replace a mapping without modifying the planner. A missing mapping causes an explicit `IllegalStateException`.

Before any score is calculated, a route is rejected if it is unavailable, has different endpoints, is prohibited for the request, exceeds the total budget, misses the deadline, or exceeds the emission limit. Exact equality with a budget, deadline, or emission limit is allowed. Endpoint matching uses `Location` record equality, including name and coordinates.

An empty candidate list or no feasible route causes `NoSuitableRouteException`. When routes exist, the exception contains their rejection reasons. Missing input, duplicate IDs, negative measurements, non-finite numbers, and invalid weights are rejected explicitly. No failure is represented by a silent `null` route.

## Runtime behavior

`Main.switchStrategies()` creates one planner with Fastest, then switches that same object to Cheapest, Eco friendly, and Balanced: three changes during one execution. `Main.automaticSelection()` demonstrates urgent, economy, eco, standard, and eco-preferred standard requests.

This is Strategy because algorithm implementations are interchangeable objects behind a common interface. The command-line `switch` only selects demonstration modes; it does not select routes or implement scoring inside the Context.

To add an algorithm, implement `RouteStrategy`, or extend `ScoringRouteStrategy` when the existing filtering and lowest-score selection fit. Add an instance to the application's comparison list, and register a selector mapping if automatic selection should use it. `RoutePlanner` needs no modification.

## Example results

For the standard request with a 6000 KZT budget, 60-minute deadline, and 10 kg emission limit:

| Strategy | Route | Time, min | Cost, KZT | Distance, km | CO2, kg | Score |
| --- | --- | ---: | ---: | ---: | ---: | ---: |
| Fastest | R2 | 22 | 5400 | 24 | 6.5 | 22.0000 |
| Cheapest | R3 | 45 | 2400 | 20 | 3.8 | 2400.0000 |
| Eco friendly | R4 | 36 | 3000 | 16 | 0.7 | 3.7600 |
| Balanced | R4 | 36 | 3000 | 16 | 0.7 | 0.3285 |
| Distance first | R5 | 35 | 3150 | 12 | 3.0 | 12.0000 |

Scores have different meanings across strategies and should only be compared within the same algorithm. R7 is unavailable; R8 exceeds the standard budget.

An urgent request with a 10000 KZT budget and 30-minute deadline selects R8 at 8375 KZT, including the urgent surcharge. An economy request limited to 2500 KZT selects R3. Changing request limits can also change the winner without changing the strategy.

An excerpt from the console:

```text
Fastest -> R2 | time 22 min | cost 5400.00 KZT | distance 24.0 km | CO2 6.50 kg | score 22.0000
  Time: 18 base + 3 traffic + 1 weather = 22 min
  Lowest score: 22.0000; equal scores are resolved by route id
```

The full [demo output](docs/demo-output.txt) includes candidate scores, rejected routes, automatic selection, comparison, and four failure scenarios.

## Design principles

**SRP:** `RoutePlanner` delegates planning, concrete strategies score routes, `StrategySelector` chooses an algorithm, and `ConsolePrinter` handles output.

**OCP:** the distance strategy extends the system through the existing interface; the Context stays unchanged. Strategy registration is confined to application setup.

**DIP:** `RoutePlanner`, `StrategySelector`, and `StrategyComparison` depend on `RouteStrategy`, not specific algorithms.

**Strategy and Bridge:** Strategy replaces an algorithm for one task, such as choosing a route. Bridge separates an abstraction from its implementation so two dimensions can vary independently, for example delivery service types and transport implementations. Both use composition, but this project's variation is the route-selection algorithm, so Strategy fits that purpose.

The shared scoring base class also uses a template method internally. This reuse does not change the Context's dependency on the Strategy interface.

## Tests

`./gradlew clean build` passed with **48 tests, 0 failures, 0 errors, and 0 skipped tests**.

| Test class | Tests | Coverage |
| --- | ---: | --- |
| `RouteStrategiesTest` | 15 | All five algorithms, full price, delays, normalization, configurable weights, ties, single route, invalid weights |
| `RouteConstraintsTest` | 15 | Availability across all strategies, budget, deadline, emissions, prohibited routes, endpoints, exact limits, missing data, no feasible route, immutable results |
| `PlanningServicesTest` | 11 | Three runtime switches, priority mappings, eco preference, registration, comparison, request changes |
| `DomainValidationTest` | 7 | Invalid numeric values, missing fields, coordinates, defensive copying |

Counts include parameterized test cases. Gradle writes the HTML report to `build/reports/tests/test/index.html` and XML results to `build/test-results/test/`.

## Limitations

The engine selects among supplied candidate routes; it does not search a road graph or solve multi-stop vehicle routing. Traffic and weather delays are fixed inputs. Prices use `double` for the exercise; monetary calculations in a production system would need a defined rounding policy and decimal arithmetic. Balanced scores depend on the feasible candidate set. The scoring base class supports minimization; an algorithm with a different selection process can implement `RouteStrategy` directly. Mutable planners and selector registrations are intended for this single-threaded console application.

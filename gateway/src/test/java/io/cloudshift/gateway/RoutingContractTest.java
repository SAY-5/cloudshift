package io.cloudshift.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import io.cloudshift.gateway.RoutingProperties.Capability;
import io.cloudshift.gateway.RoutingProperties.Target;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Properties the routing contract must hold for the strangler-fig facade. These check the
 * resolution rules directly rather than through a running gateway so the invariants are stated
 * plainly.
 */
class RoutingContractTest {

    private static final List<String> SAMPLE_PATHS =
            List.of(
                    "/reservations",
                    "/reservations/1",
                    "/reservations/1/notes",
                    "/rooms",
                    "/rooms/9",
                    "/",
                    "/anything/else",
                    "/reservationsx");

    private static RoutingProperties properties(Target reservationsTarget) {
        RoutingProperties properties = new RoutingProperties();
        properties.setMonolithUri("http://monolith:8081");
        Capability reservations = new Capability();
        reservations.setPathPrefix("/reservations");
        reservations.setServiceUri("http://reservation-service:8082");
        reservations.setTarget(reservationsTarget);
        properties.getCapabilities().put("reservations", reservations);
        return properties;
    }

    @Test
    void everyPathResolvesToExactlyOneKnownBackend() {
        for (Target target : Target.values()) {
            RoutePlan plan = RoutePlan.from(properties(target));
            for (String path : SAMPLE_PATHS) {
                String backend = plan.resolve(path);
                assertThat(backend)
                        .as("path %s with target %s", path, target)
                        .isIn("http://monolith:8081", "http://reservation-service:8082");
            }
        }
    }

    @Test
    void flippingTheTargetMovesOnlyTheMatchingCapability() {
        RoutePlan beforeCutover = RoutePlan.from(properties(Target.MONOLITH));
        RoutePlan afterCutover = RoutePlan.from(properties(Target.SERVICE));

        assertThat(beforeCutover.resolve("/reservations/1")).isEqualTo("http://monolith:8081");
        assertThat(afterCutover.resolve("/reservations/1"))
                .isEqualTo("http://reservation-service:8082");

        // A path outside the capability is unaffected by the flip.
        assertThat(beforeCutover.resolve("/rooms/9")).isEqualTo("http://monolith:8081");
        assertThat(afterCutover.resolve("/rooms/9")).isEqualTo("http://monolith:8081");
    }

    @Test
    void prefixMatchDoesNotLeakToSimilarlyNamedPaths() {
        RoutePlan plan = RoutePlan.from(properties(Target.SERVICE));

        // "/reservationsx" is not under the "/reservations" prefix.
        assertThat(plan.matchesCapability("/reservationsx")).isFalse();
        assertThat(plan.resolve("/reservationsx")).isEqualTo("http://monolith:8081");
    }
}

package io.cloudshift.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import io.cloudshift.gateway.RoutingProperties.Capability;
import io.cloudshift.gateway.RoutingProperties.Target;
import org.junit.jupiter.api.Test;

class RoutePlanTest {

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
    void routesReservationsToMonolithBeforeCutover() {
        RoutePlan plan = RoutePlan.from(properties(Target.MONOLITH));

        assertThat(plan.resolve("/reservations")).isEqualTo("http://monolith:8081");
        assertThat(plan.resolve("/reservations/5")).isEqualTo("http://monolith:8081");
    }

    @Test
    void routesReservationsToServiceAfterCutover() {
        RoutePlan plan = RoutePlan.from(properties(Target.SERVICE));

        assertThat(plan.resolve("/reservations/5")).isEqualTo("http://reservation-service:8082");
    }

    @Test
    void fallsThroughUnmappedPathsToMonolith() {
        RoutePlan plan = RoutePlan.from(properties(Target.SERVICE));

        assertThat(plan.resolve("/rooms")).isEqualTo("http://monolith:8081");
        assertThat(plan.matchesCapability("/rooms")).isFalse();
    }
}

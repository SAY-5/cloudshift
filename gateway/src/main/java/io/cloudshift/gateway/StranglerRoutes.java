package io.cloudshift.gateway;

import io.cloudshift.gateway.RoutingProperties.Capability;
import io.cloudshift.gateway.RoutingProperties.Target;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Builds the gateway route table from externalized routing state. Each declared capability becomes
 * a route to either its extracted service or the monolith, and a catch-all route sends every
 * remaining path to the monolith so traffic keeps flowing for not-yet-migrated capabilities.
 */
@Configuration
public class StranglerRoutes {

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder, RoutingProperties properties) {
        RouteLocatorBuilder.Builder routes = builder.routes();

        for (var capabilityEntry : properties.getCapabilities().entrySet()) {
            String id = capabilityEntry.getKey();
            Capability capability = capabilityEntry.getValue();
            String backend =
                    capability.getTarget() == Target.SERVICE
                            ? capability.getServiceUri()
                            : properties.getMonolithUri();
            String prefix = capability.getPathPrefix();
            routes.route(id, spec -> spec.path(prefix, prefix + "/**").uri(backend));
        }

        routes.route(
                "monolith-fallback", spec -> spec.path("/**").uri(properties.getMonolithUri()));

        return routes.build();
    }
}

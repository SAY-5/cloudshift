package io.cloudshift.gateway;

import io.cloudshift.gateway.RoutingProperties.Capability;
import io.cloudshift.gateway.RoutingProperties.Target;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Pure resolution of a request path to a single backend URI given the current routing state. Holds
 * no framework dependencies so the routing contract can be exercised directly in unit and property
 * tests.
 */
public final class RoutePlan {

    private final String monolithUri;
    private final List<Entry> entries;

    private RoutePlan(String monolithUri, List<Entry> entries) {
        this.monolithUri = monolithUri;
        this.entries = entries;
    }

    public static RoutePlan from(RoutingProperties properties) {
        List<Entry> built = new ArrayList<>();
        for (Capability capability : properties.getCapabilities().values()) {
            String backend =
                    capability.getTarget() == Target.SERVICE
                            ? capability.getServiceUri()
                            : properties.getMonolithUri();
            built.add(new Entry(capability.getPathPrefix(), backend));
        }
        return new RoutePlan(properties.getMonolithUri(), built);
    }

    /**
     * Resolves the backend URI for a path. A path matching a known capability prefix routes to that
     * capability's current backend. Every other path falls through to the monolith, so exactly one
     * backend is always selected.
     */
    public String resolve(String path) {
        return matchingEntry(path).map(entry -> entry.backend).orElse(monolithUri);
    }

    /** True when the path matches an explicitly declared capability prefix. */
    public boolean matchesCapability(String path) {
        return matchingEntry(path).isPresent();
    }

    private Optional<Entry> matchingEntry(String path) {
        return entries.stream().filter(entry -> underPrefix(path, entry.prefix)).findFirst();
    }

    private static boolean underPrefix(String path, String prefix) {
        return path.equals(prefix) || path.startsWith(prefix + "/");
    }

    private record Entry(String prefix, String backend) {}
}

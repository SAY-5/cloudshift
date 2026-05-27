package io.cloudshift.gateway;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalized routing state for the strangler-fig facade. Each capability names a path prefix and
 * a current target. Flipping a target from {@code monolith} to {@code service} cuts that capability
 * over to its extracted service without a code change.
 */
@ConfigurationProperties(prefix = "cloudshift.routing")
public class RoutingProperties {

    /** Base URL of the monolith, used for any capability still routed to it. */
    private String monolithUri = "http://localhost:8081";

    /** Per-capability configuration keyed by capability name. */
    private Map<String, Capability> capabilities = new LinkedHashMap<>();

    public String getMonolithUri() {
        return monolithUri;
    }

    public void setMonolithUri(String monolithUri) {
        this.monolithUri = monolithUri;
    }

    public Map<String, Capability> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(Map<String, Capability> capabilities) {
        this.capabilities = capabilities;
    }

    public enum Target {
        MONOLITH,
        SERVICE
    }

    public static class Capability {

        /** Path prefix the gateway matches, for example {@code /reservations}. */
        private String pathPrefix;

        /** URI of the extracted service backing this capability. */
        private String serviceUri;

        /** Current routing target for the capability. */
        private Target target = Target.MONOLITH;

        public String getPathPrefix() {
            return pathPrefix;
        }

        public void setPathPrefix(String pathPrefix) {
            this.pathPrefix = pathPrefix;
        }

        public String getServiceUri() {
            return serviceUri;
        }

        public void setServiceUri(String serviceUri) {
            this.serviceUri = serviceUri;
        }

        public Target getTarget() {
            return target;
        }

        public void setTarget(Target target) {
            this.target = target;
        }
    }
}

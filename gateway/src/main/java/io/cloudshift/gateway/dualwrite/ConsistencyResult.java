package io.cloudshift.gateway.dualwrite;

import java.util.List;

/**
 * Outcome of comparing a dual write across the monolith and the extracted service. When the two
 * records disagree the result is divergent and lists the fields that differ.
 */
public record ConsistencyResult(boolean consistent, List<String> divergentFields) {

    public static ConsistencyResult matched() {
        return new ConsistencyResult(true, List.of());
    }

    public static ConsistencyResult divergent(List<String> fields) {
        return new ConsistencyResult(false, List.copyOf(fields));
    }

    public boolean isDivergent() {
        return !consistent;
    }
}

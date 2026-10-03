package com.reservationhub.support;

/**
 * TestNG groups used for selecting subsets of the suite.
 * <ul>
 *   <li>{@code smoke}: minimal "is the core journey alive" set; fast gate on every build.</li>
 *   <li>{@code known-defect}: currently fails because of a logged bug in BUGS.md. Excluding this group
 *       (-DexcludedGroups=known-defect) gives a green gate that still catches <i>new</i> regressions
 *       while the known bugs are being fixed.</li>
 *   <li>{@code perf}: concurrency/latency smoke check against a shared sandbox; indicative, not a benchmark.</li>
 * </ul>
 */
public final class Groups {

    public static final String SMOKE = "smoke";
    public static final String KNOWN_DEFECT = "known-defect";
    public static final String PERF = "perf";

    private Groups() {
    }
}

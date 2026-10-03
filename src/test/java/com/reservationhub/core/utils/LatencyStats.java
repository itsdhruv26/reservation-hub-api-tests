package com.reservationhub.core.utils;

import com.reservationhub.controllers.apicontroller.BookingApiController.TimedCreate;
import common.extent.Logger;
import io.qameta.allure.Allure;

import java.util.List;
import java.util.stream.Collectors;

/** Latency figures for a burst of timed creates, and their summary in the Allure and Extent reports. */
public final class LatencyStats {

    private LatencyStats() {
    }

    public static long p95(List<TimedCreate> results) {
        List<Long> sorted = results.stream().map(TimedCreate::latencyMs).sorted().toList();
        return sorted.get((int) Math.ceil(0.95 * sorted.size()) - 1);
    }

    /** Attaches request count, min / p95 / max latency and the status breakdown to both reports. */
    public static void attachSummary(List<TimedCreate> results) {
        List<Long> sorted = results.stream().map(TimedCreate::latencyMs).sorted().toList();
        String summary = "requests: " + results.size()
                + "\nmin ms: " + sorted.get(0)
                + "\np95 ms: " + p95(results)
                + "\nmax ms: " + sorted.get(sorted.size() - 1)
                + "\nstatuses: " + results.stream().collect(Collectors.groupingBy(TimedCreate::status, Collectors.counting()));
        Allure.addAttachment("Latency summary", "text/plain", summary);
        Logger.logInfo("Latency summary:<br>" + summary.replace("\n", "<br>"));
    }
}

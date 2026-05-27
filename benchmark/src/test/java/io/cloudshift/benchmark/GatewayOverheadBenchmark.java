package io.cloudshift.benchmark;

import static org.assertj.core.api.Assertions.assertThat;

import io.cloudshift.gateway.GatewayApplication;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Measures the latency the gateway facade adds over calling the backend directly. The same backend
 * is hit two ways: straight, and through the running gateway. The overhead is the difference
 * between the two measured medians. A regression smoke gate fails the build if the overhead exceeds
 * the configured budget.
 */
@SpringBootTest(
        classes = GatewayApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayOverheadBenchmark {

    private static final int WARMUP = 1000;
    private static final int MEASURED = 5000;

    static final MockWebServer BACKEND = new MockWebServer();

    static {
        BACKEND.setDispatcher(
                new Dispatcher() {
                    @Override
                    public MockResponse dispatch(RecordedRequest request) {
                        return new MockResponse().setResponseCode(200).setBody("[]");
                    }
                });
        try {
            BACKEND.start();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void routing(DynamicPropertyRegistry registry) {
        registry.add(
                "cloudshift.routing.monolith-uri", () -> "http://localhost:" + BACKEND.getPort());
        registry.add(
                "cloudshift.routing.capabilities.reservations.path-prefix", () -> "/reservations");
        registry.add(
                "cloudshift.routing.capabilities.reservations.service-uri",
                () -> "http://localhost:" + BACKEND.getPort());
        registry.add("cloudshift.routing.capabilities.reservations.target", () -> "MONOLITH");
    }

    @AfterAll
    static void shutdown() throws IOException {
        BACKEND.shutdown();
    }

    @Value("${local.server.port}")
    private int gatewayPort;

    private final OkHttpClient client =
            new OkHttpClient.Builder()
                    .connectTimeout(Duration.ofSeconds(2))
                    .readTimeout(Duration.ofSeconds(5))
                    .build();

    @Test
    void facadeOverheadStaysWithinBudget() throws IOException {
        String directUrl = "http://localhost:" + BACKEND.getPort() + "/reservations";
        String gatewayUrl = "http://localhost:" + gatewayPort + "/reservations";

        sample(directUrl, WARMUP);
        sample(gatewayUrl, WARMUP);

        long[] direct = sample(directUrl, MEASURED);
        long[] gateway = sample(gatewayUrl, MEASURED);

        double directMedianMicros = medianMicros(direct);
        double gatewayMedianMicros = medianMicros(gateway);
        double directMeanMicros = meanMicros(direct);
        double gatewayMeanMicros = meanMicros(gateway);
        double overheadMicros = gatewayMedianMicros - directMedianMicros;
        double overheadFraction = overheadMicros / directMedianMicros;

        Baseline baseline = Baseline.load();
        double ceiling = baseline.overheadRatio() + baseline.tolerance();

        String report =
                String.format(
                        "# Gateway routing overhead%n%n"
                                + "Measured on %d requests after %d warmup requests, against a"
                                + " local backend.%n%n"
                                + "| Path | direct median | gateway median | direct mean |"
                                + " gateway mean |%n"
                                + "| --- | --- | --- | --- | --- |%n"
                                + "| /reservations | %.0f us | %.0f us | %.0f us | %.0f us |%n%n"
                                + "Median overhead added by the facade: %.0f us"
                                + " (%.1f%% over the direct median).%n%n"
                                + "Baseline overhead ratio %.1f%%, regression tolerance %.0f%%,"
                                + " gate ceiling %.1f%%.%n",
                        MEASURED,
                        WARMUP,
                        directMedianMicros,
                        gatewayMedianMicros,
                        directMeanMicros,
                        gatewayMeanMicros,
                        overheadMicros,
                        overheadFraction * 100,
                        baseline.overheadRatio() * 100,
                        baseline.tolerance() * 100,
                        ceiling * 100);

        Path out = Path.of("target", "benchmark-report.md");
        Files.createDirectories(out.getParent());
        Files.writeString(out, report);
        System.out.print(report);

        assertThat(overheadFraction)
                .as(
                        "measured facade overhead ratio against the recorded baseline plus"
                                + " tolerance")
                .isLessThanOrEqualTo(ceiling);
    }

    private long[] sample(String url, int count) throws IOException {
        Request request = new Request.Builder().url(url).build();
        long[] timings = new long[count];
        for (int i = 0; i < count; i++) {
            long start = System.nanoTime();
            try (Response response = client.newCall(request).execute()) {
                response.body().bytes();
            }
            timings[i] = System.nanoTime() - start;
        }
        return timings;
    }

    private static double medianMicros(long[] timings) {
        List<Long> sorted = new ArrayList<>();
        for (long timing : timings) {
            sorted.add(timing);
        }
        Collections.sort(sorted);
        long nanos = sorted.get(sorted.size() / 2);
        return TimeUnit.NANOSECONDS.toNanos(nanos) / 1000.0;
    }

    private static double meanMicros(long[] timings) {
        long total = 0;
        for (long timing : timings) {
            total += timing;
        }
        return (total / (double) timings.length) / 1000.0;
    }

    private record Baseline(double overheadRatio, double tolerance) {

        static Baseline load() {
            Properties properties = new Properties();
            try (var in = Files.newInputStream(Path.of("baseline.properties"))) {
                properties.load(in);
            } catch (IOException e) {
                throw new IllegalStateException("baseline.properties is required", e);
            }
            return new Baseline(
                    Double.parseDouble(properties.getProperty("overhead.ratio")),
                    Double.parseDouble(properties.getProperty("regress.tolerance")));
        }
    }
}

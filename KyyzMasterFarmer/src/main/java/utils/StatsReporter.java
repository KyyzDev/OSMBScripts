package utils;

import java.net.URI;
import java.net.http.*;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Supplier;

public class StatsReporter {
    private static final String API_URL = "https://osmb.kyyz.dev/api/stats";
    private final String scriptId;
    private final String sessionId;
    private final HttpClient client;
    private ScheduledExecutorService scheduler;

    public StatsReporter(String scriptId) {
        this.scriptId = scriptId;
        this.sessionId = UUID.randomUUID().toString();
        this.client = HttpClient.newHttpClient();
    }

    public void start(Supplier<Map<String, Object>> statsSupplier) {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> sendStats(statsSupplier.get()), 0, 30, TimeUnit.SECONDS);
    }

    public void stop() {
        if (scheduler != null) scheduler.shutdown();
    }

    private void sendStats(Map<String, Object> stats) {
        try {
            String json = String.format(
                "{\"scriptId\":\"%s\",\"sessionId\":\"%s\",\"stats\":%s}",
                scriptId, sessionId, mapToJson(stats)
            );
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
            client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) { /* silent */ }
    }

    private String mapToJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        map.forEach((k, v) -> {
            if (v instanceof String) {
                sb.append("\"").append(k).append("\":\"").append(v).append("\",");
            } else {
                sb.append("\"").append(k).append("\":").append(v).append(",");
            }
        });
        if (sb.length() > 1) sb.setLength(sb.length() - 1);
        return sb.append("}").toString();
    }
}

package com.trident.harbour.demo;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.trident.harbour.model.HarbourGraph;
import com.trident.harbour.service.BaselineStrategy;
import com.trident.harbour.service.CriticalityPerHourStrategy;
import com.trident.harbour.service.IntegratedStrategy;
import com.trident.harbour.service.RepairSimulator;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Local server for the What-if tab. Run from the project root so it can serve dashboard.html and
 * dashboard_data.js. Listens on 127.0.0.1 only.
 *
 * GET /api/simulate?faults=3:80,4:95&crews=2
 *   faults = assetId:damage pairs (damage 1..100; baseline repairs the most damaged first), crews = 1..3
 *   Runs the REAL RepairSimulator with the baseline, integrated and rule-only strategies on a fresh harbour.
 */
public final class HarbourServer {

    private static final int PORT = 8080;
    private static final Set<String> STATIC_FILES = Set.of("dashboard.html", "dashboard_data.js");

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", PORT), 0);
        server.createContext("/api/simulate", HarbourServer::simulate);
        server.createContext("/", HarbourServer::staticFile);
        server.start();
        System.out.println("TRIDENT server running: http://localhost:" + PORT + "/dashboard.html   (Ctrl+C to stop)");
    }

    private static void simulate(HttpExchange ex) throws IOException {
        try {
            Map<String, String> q = query(ex.getRequestURI().getRawQuery());
            int crews = Integer.parseInt(q.getOrDefault("crews", "1").trim());
            if (crews < 1 || crews > 3) {
                throw new IllegalArgumentException("crews must be 1, 2 or 3");
            }

            HarbourGraph g = AppendixAHarbour.build();
            List<Integer> faults = new ArrayList<>();
            for (String part : q.getOrDefault("faults", "").split(",")) {
                if (part.isBlank()) continue;
                String[] kv = part.split(":");
                int id = Integer.parseInt(kv[0].trim());
                double damage = kv.length > 1 ? Double.parseDouble(kv[1].trim()) : 80.0;
                if (!g.hasAsset(id)) throw new IllegalArgumentException("unknown asset " + id);
                if (faults.contains(id)) throw new IllegalArgumentException("asset " + id + " listed twice");
                if (damage < 1 || damage > 100) throw new IllegalArgumentException("damage must be 1..100");
                g.getAsset(id).applyDamage(damage);
                faults.add(id);
            }
            if (faults.isEmpty()) {
                throw new IllegalArgumentException("choose at least one fault");
            }

            Map<Integer, Double> conf = new HashMap<>();
            for (int id : faults) conf.put(id, 1.0);
            RepairSimulator sim = new RepairSimulator();

            String json = "{\"label\":\"WHAT-IF\",\"crews\":" + crews + ",\"faults\":" + faults
                    + ",\"strategies\":{"
                    + "\"BASELINE\":" + DashboardExporter.strategy(sim, g, new BaselineStrategy(), faults, conf, crews)
                    + ",\"INTEGRATED\":" + DashboardExporter.strategy(sim, g, new IntegratedStrategy(), faults, conf, crews)
                    + ",\"RULE_ONLY\":" + DashboardExporter.strategy(sim, g, new CriticalityPerHourStrategy(), faults, conf, crews)
                    + "}}";
            send(ex, 200, "application/json", json.getBytes(StandardCharsets.UTF_8));
        } catch (RuntimeException e) {
            String msg = String.valueOf(e.getMessage()).replace("\\", "\\\\").replace("\"", "\\\"");
            send(ex, 400, "application/json", ("{\"error\":\"" + msg + "\"}").getBytes(StandardCharsets.UTF_8));
        }
    }

    private static void staticFile(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        String name = path.equals("/") ? "dashboard.html" : path.substring(1);
        Path file = Path.of(name);
        if (!STATIC_FILES.contains(name) || !Files.exists(file)) {
            send(ex, 404, "text/plain", "Not found. Start the server from the project root.".getBytes(StandardCharsets.UTF_8));
            return;
        }
        String type = name.endsWith(".html") ? "text/html" : "application/javascript";
        send(ex, 200, type, Files.readAllBytes(file));
    }

    private static Map<String, String> query(String raw) {
        Map<String, String> m = new HashMap<>();
        if (raw == null) return m;
        for (String pair : raw.split("&")) {
            int i = pair.indexOf('=');
            if (i < 0) continue;
            m.put(URLDecoder.decode(pair.substring(0, i), StandardCharsets.UTF_8),
                    URLDecoder.decode(pair.substring(i + 1), StandardCharsets.UTF_8));
        }
        return m;
    }

    private static void send(HttpExchange ex, int code, String type, byte[] body) throws IOException {
        ex.getResponseHeaders().set("Content-Type", type + "; charset=utf-8");
        ex.sendResponseHeaders(code, body.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(body);
        }
    }
}
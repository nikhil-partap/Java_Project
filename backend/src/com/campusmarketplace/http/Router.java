package com.campusmarketplace.http;

import com.campusmarketplace.exception.ApiException;
import com.campusmarketplace.exception.NotFoundException;
import com.campusmarketplace.exception.ValidationException;
import com.campusmarketplace.service.MarketplaceService;
import com.campusmarketplace.thread.MarketplaceMonitorThread;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The single HTTP entry point. INTERFACE: implements the JDK's HttpHandler.
 * 1. adds CORS headers, answers OPTIONS with 204,
 * 2. ROUTES "METHOD /path" to a service call,
 * 3. is the ONE CENTRAL EXCEPTION HANDLER: every exception thrown anywhere below
 *    propagates up to handle(), where try/catch/finally turns it into {"error": "..."}.
 * Each request runs on a thread of the fixed thread pool given to HttpServer.
 */
public class Router implements HttpHandler {

    private static final int MAX_BODY_BYTES = 20 * 1024 * 1024; // 20 MB (base64 images)

    private final MarketplaceService service;
    private final MarketplaceMonitorThread monitor;

    public Router(MarketplaceService service, MarketplaceMonitorThread monitor) {
        this.service = service;
        this.monitor = monitor;
    }

    /** STATIC NESTED CLASS: a status code + body pair returned by route(). */
    private static class Response {
        final int status;
        final Object body;

        Response(int status, Object body) {
            this.status = status;
            this.body = body;
        }
    }

    @Override
    public void handle(HttpExchange exchange) {
        long start = System.currentTimeMillis();
        String method = exchange.getRequestMethod().toUpperCase();
        String path = exchange.getRequestURI().getPath();
        int status = 500;
        try {
            addCorsHeaders(exchange);
            if (method.equals("OPTIONS")) { // CORS pre-flight
                status = 204;
                exchange.sendResponseHeaders(204, -1);
                return; // "finally" still runs
            }
            Response response = route(method, path, exchange);
            status = response.status;
            send(exchange, status, response.body);
        } catch (ApiException e) {                              // our own exceptions (400/401/403/404/409...)
            status = e.getStatus();
            sendError(exchange, status, e.getMessage());
        } catch (NumberFormatException | ClassCastException e) { // MULTI-CATCH: bad input types
            status = 400;
            sendError(exchange, status, "Bad request data: " + e.getMessage());
        } catch (Exception e) {                                  // anything unexpected -> 500
            status = 500;
            e.printStackTrace();
            sendError(exchange, status, "Internal server error: " + e);
        } finally {
            // FINALLY: always runs - log the request and release the connection.
            System.out.println(String.format("%-7s %-40s -> %d (%d ms) [%s]", method, path, status,
                    System.currentTimeMillis() - start, Thread.currentThread().getName()));
            exchange.close();
        }
    }

    private Response route(String method, String path, HttpExchange ex) throws ApiException, IOException {
        List<String> p = new ArrayList<>();
        for (String part : path.split("/")) {
            if (!part.isEmpty()) p.add(part);
        }
        if (p.isEmpty() || !p.get(0).equals("api") || p.size() < 2) {
            throw new NotFoundException("Route not found: " + path);
        }
        String resource = p.get(1);
        int size = p.size();

        switch (resource) {
            case "health":
                if (size == 2) {
                    allow(method, "GET");
                    return new Response(200, Map.of("status", "ok"));
                }
                break;
            case "state":
                if (size == 2) {
                    allow(method, "GET");
                    return new Response(200, service.getState());
                }
                break;
            case "auth":
                if (size == 3 && p.get(2).equals("login")) {
                    allow(method, "POST");
                    return new Response(200, service.login(readBody(ex)));
                }
                if (size == 3 && p.get(2).equals("register")) {
                    allow(method, "POST");
                    return new Response(201, service.register(readBody(ex)));
                }
                break;
            case "users":
                if (size == 3) {
                    allow(method, "PUT");
                    return new Response(200, service.updateUser(p.get(2), readBody(ex)));
                }
                if (size == 4 && p.get(3).equals("toggle-block")) {
                    allow(method, "POST");
                    return new Response(200, service.toggleBlock(p.get(2)));
                }
                break;
            case "listings":
                if (size == 2) {
                    allow(method, "GET", "POST");
                    if (method.equals("GET")) {
                        Map<String, String> q = query(ex);
                        return new Response(200, service.getListings(q.get("category"), q.get("search"), q.get("sort")));
                    }
                    return new Response(201, service.createListing(readBody(ex)));
                }
                if (size == 3) {
                    allow(method, "PUT", "DELETE");
                    if (method.equals("PUT")) {
                        return new Response(200, service.updateListing(p.get(2), readBody(ex)));
                    }
                    service.deleteListing(p.get(2));
                    return new Response(200, Map.of("deleted", true));
                }
                if (size == 4 && p.get(3).equals("requests")) {
                    allow(method, "POST");
                    return new Response(200, service.addRequest(p.get(2), readBody(ex)));
                }
                if (size == 4 && p.get(3).equals("purchase")) {
                    allow(method, "POST");
                    return new Response(200, service.purchase(p.get(2), readBody(ex)));
                }
                break;
            case "purchases":
                if (size == 2) {
                    allow(method, "GET");
                    return new Response(200, service.getPurchases());
                }
                break;
            case "stats":
                if (size == 2) {
                    allow(method, "GET");
                    return new Response(200, monitor.getSnapshot());
                }
                break;
            default:
                break;
        }
        throw new NotFoundException("Route not found: " + path);
    }

    /** Throws 405 when the path exists but the HTTP method is wrong. VARARGS parameter. */
    private static void allow(String method, String... allowed) throws ApiException {
        for (String a : allowed) {
            if (a.equals(method)) return;
        }
        throw new ApiException(405, "Method " + method + " not allowed here.");
    }

    /** Reads the request body and parses it as a JSON object ({} when empty). */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> readBody(HttpExchange ex) throws ApiException, IOException {
        byte[] bytes;
        try (InputStream in = ex.getRequestBody()) { // TRY-WITH-RESOURCES closes the stream
            bytes = in.readNBytes(MAX_BODY_BYTES + 1);
        }
        if (bytes.length > MAX_BODY_BYTES) {
            throw new ApiException(413, "Request body too large (max 20 MB).");
        }
        String text = new String(bytes, StandardCharsets.UTF_8).trim();
        if (text.isEmpty()) {
            return new HashMap<>();
        }
        Object parsed = Json.parse(text);
        if (!(parsed instanceof Map)) {
            throw new ValidationException("Request body must be a JSON object.");
        }
        return (Map<String, Object>) parsed;
    }

    /** Parses "?category=Books&search=lamp" into a map. */
    private static Map<String, String> query(HttpExchange ex) {
        Map<String, String> params = new HashMap<>();
        String raw = ex.getRequestURI().getRawQuery();
        if (raw == null) return params;
        for (String pair : raw.split("&")) {
            int eq = pair.indexOf('=');
            String key = eq < 0 ? pair : pair.substring(0, eq);
            String value = eq < 0 ? "" : pair.substring(eq + 1);
            params.put(URLDecoder.decode(key, StandardCharsets.UTF_8), URLDecoder.decode(value, StandardCharsets.UTF_8));
        }
        return params;
    }

    private static void addCorsHeaders(HttpExchange ex) {
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
        ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    private static void send(HttpExchange ex, int status, Object body) throws IOException {
        byte[] bytes = Json.write(body).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static void sendError(HttpExchange ex, int status, String message) {
        try {
            send(ex, status, Map.of("error", message == null ? "Unknown error" : message));
        } catch (IOException e) {
            System.err.println("Could not send error response: " + e.getMessage());
        }
    }
}

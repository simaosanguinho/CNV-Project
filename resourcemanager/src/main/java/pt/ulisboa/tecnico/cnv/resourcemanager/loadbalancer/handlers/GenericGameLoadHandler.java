package pt.ulisboa.tecnico.cnv.resourcemanager.loadbalancer.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import pt.ulisboa.tecnico.cnv.resourcemanager.common.Instance;
import pt.ulisboa.tecnico.cnv.resourcemanager.common.InstancePool;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import pt.ulisboa.tecnico.cnv.mss.MSS;

public abstract class GenericGameLoadHandler implements HttpHandler {

    protected InstancePool instancePool;
    protected MSS mss;

    public GenericGameLoadHandler(InstancePool instancePool, MSS mss) {
        this.instancePool = instancePool;
        this.mss = mss;
    }

    /***
     * Parse the HTTP request from the HttpExchange object.
     * Extracts the query parameters and returns them as a map.
     */
    protected Map<String, String> parseRequest(HttpExchange he) {
        URI requestedUri = he.getRequestURI();
        String query = requestedUri.getRawQuery();
        return queryToMap(query);
    }

    /**
     * Parse query string into a map with workload.
     */
    private Map<String, String> queryToMap(String query) {
        if (query == null) {
            return null;
        }
        Map<String, String> result = new HashMap<>();
        for (String param : query.split("&")) {
            String[] entry = param.split("=");
            if (entry.length > 1) {
                result.put(entry[0], entry[1]);
            } else {
                result.put(entry[0], "");
            }
        }
        return result;
    }

    protected String routeRequestToWorker(Map<String, String> workload, double complexity) {
        Optional<Instance> chosenWorker = this.chooseWorker(complexity);
        if (chosenWorker.isEmpty()) {
            return "Could not find worker instance for request";
        }

        // Dispatch the request to the worker instance
        Instance worker = chosenWorker.get();
        String baseUrl = chosenWorker.get().getPublicIpAddress();
        worker.incrementAccumulatedComplexity(complexity);
        Optional<HttpResponse<String>> response = dispatchRequest(workload, baseUrl);

        if (response.isPresent()) {
            worker.decrementAccumulatedComplexity(complexity);
            return response.get().body();
        } else {
            worker.decrementAccumulatedComplexity(complexity);
            return "Error while dispatching request.";
        }
    }

    /***
     * Decides if it should send the request to an aws lambda or to a worker from
     * the instance pool
     * @param complexity the estimated cost of the request
     * @return the instance or empty if it could not find any
     */
    private Optional<Instance> chooseWorker(double complexity) {
        // TODO -> implement lambda functions
        return instancePool.selectInstanceForRequest();
    }

    /***
     * Dispatch request to the worker instance.
     * Returns an Optional containing the HttpResponse if successful, or an empty Optional if an error occurs.
     */
    private Optional<HttpResponse<String>> dispatchRequest(Map<String, String> workload, String baseUrl) {
        String query = mapToQuery(workload);
        String fullUrl = baseUrl + "?" + query;

        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(fullUrl))
                    .GET()
                    .build();
            return Optional.of(client.send(request, HttpResponse.BodyHandlers.ofString()));

        } catch (IOException |
                 InterruptedException e) {
            System.out.println("Error while dispatching request: " + e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Parse map with workload to query string for HTTP request.
     */
    private String mapToQuery(Map<String, String> workload) {
        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, String> entry : workload.entrySet()) {
            if (result.length() > 0) {
                result.append("&");
            }
            result.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        return result.toString();
    }
}

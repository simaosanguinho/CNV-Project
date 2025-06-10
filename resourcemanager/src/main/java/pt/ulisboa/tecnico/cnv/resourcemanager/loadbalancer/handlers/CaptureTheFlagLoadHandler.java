package pt.ulisboa.tecnico.cnv.resourcemanager.loadbalancer.handlers;

import com.sun.net.httpserver.HttpExchange;

import pt.ulisboa.tecnico.cnv.resourcemanager.common.InstancePool;
import pt.ulisboa.tecnico.cnv.resourcemanager.loadbalancer.estimators.CaptureTheFlagEstimator;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import pt.ulisboa.tecnico.cnv.mss.MSS;

public class CaptureTheFlagLoadHandler extends GenericGameLoadHandler {

    private final CaptureTheFlagEstimator estimator;

    public CaptureTheFlagLoadHandler(InstancePool instancePool) {
        super(instancePool);
        this.estimator = new CaptureTheFlagEstimator();
    }

    @Override
    public void handle(HttpExchange he) throws IOException {
        // Handling CORS.
        he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        if ("OPTIONS".equalsIgnoreCase(he.getRequestMethod())) {
            he.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");
            he.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type,Authorization");
            he.sendResponseHeaders(204, -1);
            return;
        }

        Map<String, String> parameters = parseRequest(he);
        int gridSize = Integer.parseInt(parameters.get("gridSize"));
        int numBlueAgents = Integer.parseInt(parameters.get("numBlueAgents"));
        int numRedAgents = Integer.parseInt(parameters.get("numRedAgents"));
        char flagPlacementType = parameters.get("flagPlacementType").toUpperCase().charAt(0);

        if (!validateInputs(gridSize, numBlueAgents, numRedAgents, flagPlacementType)) {
            String response = "Invalid input. Please provide a valid grid size, number of blue agents, number of red agents and flag placement type (A, B or C).";
            he.sendResponseHeaders(400, response.length());
            OutputStream os = he.getResponseBody();
            os.write(response.getBytes());
            os.close();
            return;
        }

        // TODO -- uncomment after
        // estimate complexity of the request
        //double estimatedCost = estimator.estimateCost(gridSize, numBlueAgents, numRedAgents, flagPlacementType);

        // route the request to a worker/lambda, receive the response
        //String response = routeRequestToWorker(parameters, estimatedCost);

        // send response to the client
        he.sendResponseHeaders(200, 0);
        OutputStream os = he.getResponseBody();
        os.write("OK 200".getBytes());
        os.close();
    }

    private boolean validateInputs(int gridSize, int numBlueAgents, int numRedAgents, char flagPlacementType) {
        if (gridSize < 10) {
            return false;
        }

        if (numBlueAgents > gridSize || numRedAgents > gridSize) {
            return false;
        }

        return List.of('A', 'B', 'C').contains(flagPlacementType);
    }
}

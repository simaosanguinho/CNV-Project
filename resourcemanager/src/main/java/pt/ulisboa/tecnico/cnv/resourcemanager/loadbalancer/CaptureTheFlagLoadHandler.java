package pt.ulisboa.tecnico.cnv.resourcemanager.loadbalancer;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import pt.ulisboa.tecnico.cnv.resourcemanager.common.InstancePool;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CaptureTheFlagLoadHandler extends GenericGameLoadHandler {

    CaptureTheFlagLoadHandler(InstancePool instancePool) {
        super(instancePool);
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

        // TODO route the request to a worker
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

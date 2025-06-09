package pt.ulisboa.tecnico.cnv.resourcemanager.loadbalancer.handlers;

import com.sun.net.httpserver.HttpExchange;
import pt.ulisboa.tecnico.cnv.resourcemanager.common.InstancePool;
import pt.ulisboa.tecnico.cnv.resourcemanager.loadbalancer.estimators.GameOfLifeEstimator;
import pt.ulisboa.tecnico.cnv.mss.MSS;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

public class GameOfLifeLoadHandler extends GenericGameLoadHandler {

    private final GameOfLifeEstimator estimator;


    public GameOfLifeLoadHandler(InstancePool instancePool, MSS mss) {
        super(instancePool, mss);
        this.estimator = new GameOfLifeEstimator();
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
        int iterations = Integer.parseInt(parameters.get("iterations"));
        String mapFilename = parameters.get("mapFilename");

        // TODO -> uncomment after
        // estimate complexity of the request
        // double estimatedCost = estimator.estimateCost(iterations, mapFilename);

        // route the request to a worker/lambda, receive the response
        // String response = routeRequestToWorker(parameters, estimatedCost);

        // TODO send response to the client
        he.sendResponseHeaders(200, 0);
        OutputStream os = he.getResponseBody();
        os.write("OK 200".getBytes());
        os.close();
    }
}

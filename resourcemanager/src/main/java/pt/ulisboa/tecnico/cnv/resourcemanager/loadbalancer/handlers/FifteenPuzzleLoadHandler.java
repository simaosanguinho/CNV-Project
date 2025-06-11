package pt.ulisboa.tecnico.cnv.resourcemanager.loadbalancer.handlers;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import pt.ulisboa.tecnico.cnv.resourcemanager.common.InstancePool;
import pt.ulisboa.tecnico.cnv.resourcemanager.loadbalancer.estimators.FifteenPuzzleEstimator;

public class FifteenPuzzleLoadHandler extends GenericGameLoadHandler {

  private final FifteenPuzzleEstimator estimator;

  public FifteenPuzzleLoadHandler(InstancePool instancePool) {
    super(instancePool);
    this.estimator = new FifteenPuzzleEstimator();
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
    int size = Integer.parseInt(parameters.get("size"));
    int shuffles = Integer.parseInt(parameters.get("shuffles"));

    // TODO -> uncomment after
    // estimate complexity of the request
    double estimatedCost = estimator.estimateCost(size, shuffles);

    // route the request to a worker/lambda, receive the response
    String response = routeRequestToWorker(parameters, estimatedCost);

    // TODO send response to the client
    he.sendResponseHeaders(200, 0);
    OutputStream os = he.getResponseBody();
    os.write(response.getBytes());
    os.close();
  }
}

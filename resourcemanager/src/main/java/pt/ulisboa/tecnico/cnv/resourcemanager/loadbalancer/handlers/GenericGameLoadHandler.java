package pt.ulisboa.tecnico.cnv.resourcemanager.loadbalancer.handlers;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import pt.ulisboa.tecnico.cnv.resourcemanager.common.Instance;
import pt.ulisboa.tecnico.cnv.resourcemanager.common.InstancePool;
import pt.ulisboa.tecnico.cnv.resourcemanager.common.LambdaPool;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.awssdk.services.lambda.model.LambdaException;




public abstract class GenericGameLoadHandler implements HttpHandler {

  protected InstancePool instancePool;
  protected final long CTF_TRESHOLD = 7435999; // 74.359990000 seconds
  protected final long FIFTEEN_TRESHOLD = 3894330;
  protected final long GOL_TRESHOLD = 3899946;
  protected LambdaPool lambdaPool;
  LambdaClient awsLambda = LambdaClient.builder()
      .region(software.amazon.awssdk.regions.Region.EU_WEST_1) // Set your AWS region here
      .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
      .build();

  
  public GenericGameLoadHandler(InstancePool instancePool) {
    
    this.instancePool = instancePool;
    this.lambdaPool = new LambdaPool(
        "capturetheflag-lambda",
        "gameoflife-lambda",
        "fifteenpuzzle-lambda"
    );
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

  /** Parse query string into a map with workload. */
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

  protected String routeRequestToWorker(Map<String, String> workload, double complexity, String game) {
    Pair<String, Optional<Instance>> chosenWorker = this.chooseWorker(complexity, game);
    if (chosenWorker.getRight().isEmpty()) {
      return "Could not find worker instance for request";
    }
    // Lambda Worker
    if (chosenWorker.getLeft().equals("lambda")) {
      // If the chosen worker is a lambda, we can directly return the lambda's name
      System.out.println("Dispatching request to Lambda worker for game: " + game);
      String response = dispatchRequestLambda(workload, game);
      if (response != null) {
        return response;
      } else {
        return "Error while dispatching request to Lambda worker.";
      }
    }
    // VM Worker
    else {
    System.out.println("Dispatching request to VM worker for game: " + game);
      // Dispatch the request to the worker instance
    Instance worker = chosenWorker.getRight().get();
    String baseUrl = chosenWorker.getRight().get().getPublicIpAddress();
    worker.incrementAccumulatedComplexity(complexity);
    Optional<HttpResponse<String>> response = dispatchRequestVM(workload, baseUrl, game);

    if (response.isPresent()) {
      worker.decrementAccumulatedComplexity(complexity);
      return response.get().body();
    } else {
      worker.decrementAccumulatedComplexity(complexity);
      return "Error while dispatching request to VM worker.";
    }
    }
    
  }

  /***
   * Decides if it should send the request to an aws lambda or to a worker from
   * the instance pool
   * @param complexity the estimated cost of the request
   * @return the instance and its type or empty if it could not find any
   */
  private Pair<String, Optional<Instance>> chooseWorker(double complexity, String game) {
    // TODO -> implement lambda functions
    if (game.equals("capturetheflag") && complexity < CTF_TRESHOLD) {
      return Pair.of("lambda", Optional.ofNullable(lambdaPool.getCtfLambda()));
    } else if (game.equals("gameoflife") && complexity < GOL_TRESHOLD) {
      return Pair.of("lambda", Optional.ofNullable(lambdaPool.getGolLambda()));
    } else if (game.equals("fifteenpuzzle") && complexity < FIFTEEN_TRESHOLD) {
      return Pair.of("lambda", Optional.ofNullable(lambdaPool.getFifteenPuzzleLambda()));
    }

    return Pair.of("vm", instancePool.selectInstanceForRequest());
  }

  /***
   * Dispatch request to the worker instance.
   * Returns an Optional containing the HttpResponse if successful, or an empty Optional if an error occurs.
   */
  private Optional<HttpResponse<String>> dispatchRequestVM(
      Map<String, String> workload, String baseUrl, String game) {
    String query = mapToQuery(workload);
    String fullUrl = "http://" + baseUrl + ":8000/" + game + "?" + query;
    System.out.println("Dispatching request to " + fullUrl);
    HttpClient client = HttpClient.newHttpClient();
    try {
      HttpRequest request = HttpRequest.newBuilder().uri(URI.create(fullUrl)).GET().build();
      
      return Optional.of(client.send(request, HttpResponse.BodyHandlers.ofString()));

    } catch (IOException | InterruptedException e) {
      System.out.println("Error while dispatching request: " + e.getMessage());
    }
    return Optional.empty();
  }

 private String dispatchRequestLambda(Map<String, String> workload, String game) {
    try {

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(workload);

        SdkBytes payload = SdkBytes.fromUtf8String(json);
        String functionName = game + "-lambda";
/*         System.out.println("Dispatching request to Lambda function: " + functionName + " with payload: " + json); */

        InvokeRequest request = InvokeRequest.builder()
            .functionName(functionName)
            .payload(payload)
            .build();

        // Invoke Lambda
        InvokeResponse res = awsLambda.invoke(request);
        String value = res.payload().asUtf8String();
        System.out.println(value);
        return value;

    } catch (LambdaException e) {
        System.err.println(e.getMessage());
        return null;
    } catch (Exception e) {
        e.printStackTrace(); // handle JSON or other unexpected errors
    }
    return null;
}

  /** Parse map with workload to query string for HTTP request. */
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

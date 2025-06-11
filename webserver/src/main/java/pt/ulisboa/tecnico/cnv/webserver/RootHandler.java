package pt.ulisboa.tecnico.cnv.webserver;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class RootHandler implements HttpHandler, RequestHandler<Map<String, String>, String> {

  private final String HELLO_MSG = "Default Web Server<br>";

  @Override
  public void handle(HttpExchange he) throws IOException {
    // Handling CORS
    he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
    if (he.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
      he.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");
      he.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type,Authorization");
      he.sendResponseHeaders(204, -1);
      return;
    }

    // parse request
    URI requestedUri = he.getRequestURI();
    String query = requestedUri.getRawQuery();
    if (query == null) query = HELLO_MSG;

    // Return a simple OK 200 response
    he.sendResponseHeaders(200, 0);
    OutputStream os = he.getResponseBody();
    os.write("OK 200".getBytes());
    os.close();
  }

  @Override
  public String handleRequest(Map<String, String> event, Context context) {
    // For AWS Lambda invocation, return a simple OK 200 response
    return "OK 200";
  }

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
}

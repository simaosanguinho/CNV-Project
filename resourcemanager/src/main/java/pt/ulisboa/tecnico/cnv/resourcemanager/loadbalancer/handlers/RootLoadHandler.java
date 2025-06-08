package pt.ulisboa.tecnico.cnv.resourcemanager.loadbalancer.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class RootLoadHandler implements HttpHandler {

     private final String HELLO_MSG = String.join("\n",
                "Hello from the Load Balancer!",
                "",
                "Available game endpoints:",
                "",
                "1. /gameoflife",
                "   Example: /gameoflife?mapFilename=glider-10-10.json&iterations=10",
                "",
                "2. /fifteenpuzzle",
                "   Example: /fifteenpuzzle?size=10&shuffles=120",
                "",
                "3. /capturetheflag",
                "   Example: /capturetheflag?gridSize=20&numBlueAgents=5&numRedAgents=5&flagPlacementType=A",
                "",
                "Send a GET request to any of the above paths with the required query parameters to run a game.",
                "",
                "Happy testing!",
                ""
    );

    @Override
    public void handle(HttpExchange he) throws IOException {
        // Handling CORS
        he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        if (he.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            he.getResponseHeaders().add("Access-Control-Allow-Methods",
                    "GET, OPTIONS");
            he.getResponseHeaders().add("Access-Control-Allow-Headers",
                    "Content-Type,Authorization");
            he.sendResponseHeaders(204, -1);
            return;
        }

        byte[] bytes = HELLO_MSG.getBytes(StandardCharsets.UTF_8);
        he.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        he.sendResponseHeaders(200, bytes.length);

        try (OutputStream os = he.getResponseBody()) {
            os.write(bytes);
        }
    }
}

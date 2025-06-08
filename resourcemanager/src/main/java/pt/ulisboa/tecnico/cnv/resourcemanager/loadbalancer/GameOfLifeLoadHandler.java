package pt.ulisboa.tecnico.cnv.resourcemanager.loadbalancer;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import pt.ulisboa.tecnico.cnv.resourcemanager.common.InstancePool;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import java.io.IOException;

public class GameOfLifeLoadHandler extends GenericGameLoadHandler {

    public GameOfLifeLoadHandler(InstancePool instancePool) {
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

        // Parse request.
        URI requestedUri = he.getRequestURI();
        String query = requestedUri.getRawQuery();
        Map<String, String> parameters = queryToMap(query);

        int iterations = Integer.parseInt(parameters.get("iterations"));
        String mapFilename = parameters.get("mapFilename");

        // TODO route the request to a worker
        he.sendResponseHeaders(200, 0);
        OutputStream os = he.getResponseBody();
        os.write("OK 200".getBytes());
        os.close();
    }
}

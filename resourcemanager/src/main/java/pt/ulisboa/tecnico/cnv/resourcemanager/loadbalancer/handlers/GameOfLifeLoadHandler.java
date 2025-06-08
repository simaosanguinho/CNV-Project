package pt.ulisboa.tecnico.cnv.resourcemanager.loadbalancer.handlers;

import com.sun.net.httpserver.HttpExchange;
import pt.ulisboa.tecnico.cnv.resourcemanager.common.InstancePool;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

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

        Map<String, String> parameters = parseRequest(he);
        int iterations = Integer.parseInt(parameters.get("iterations"));
        String mapFilename = parameters.get("mapFilename");

        // TODO route the request to a worker
        he.sendResponseHeaders(200, 0);
        OutputStream os = he.getResponseBody();
        os.write("OK 200".getBytes());
        os.close();
    }
}

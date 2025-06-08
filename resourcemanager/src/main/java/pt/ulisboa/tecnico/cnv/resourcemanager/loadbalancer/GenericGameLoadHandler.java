package pt.ulisboa.tecnico.cnv.resourcemanager.loadbalancer;

import com.sun.net.httpserver.HttpHandler;
import pt.ulisboa.tecnico.cnv.resourcemanager.common.InstancePool;

import java.util.HashMap;
import java.util.Map;

public abstract class GenericGameLoadHandler implements HttpHandler {

    protected InstancePool instancePool;

    public GenericGameLoadHandler(InstancePool instancePool) {
        this.instancePool = instancePool;
    }

    /**
     * Parse query string into a map.
     */
    protected Map<String, String> queryToMap(String query) {
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

package pt.ulisboa.tecnico.cnv.resourcemanager.loadbalancer.parsers;

import java.util.Map;
import java.util.Optional;

public interface RegressionDataParser {
    class Points {
        double[][] inputs;
        double[] outputs;
    }
    Optional<Points> parseMap(Map<String, String> workload);
    Optional<Points> parseDefault();
}

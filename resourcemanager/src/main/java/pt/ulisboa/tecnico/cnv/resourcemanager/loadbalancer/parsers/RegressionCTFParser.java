package pt.ulisboa.tecnico.cnv.resourcemanager.loadbalancer.parsers;

import java.io.*;
import java.util.*;
import java.util.Map;
import java.util.Optional;

public class RegressionCTFParser implements RegressionDataParser {

    private final String DEFAULT_FILE = "ctf_results_with_metrics.csv";

    public Double resolveFlagPlacementType(char type) {
        return type == 'A' ? 1d : (type == 'B' ? 2d : 3d);
    }

    public Optional<Points> parseMap(Map<String, String> workload) {
        // TODO
        return Optional.empty();
    }

    public Optional<Points> parseDefault() {
        try {
            InputStream is = this.getClass().getClassLoader().getResourceAsStream(DEFAULT_FILE);
            if (is == null) {
                return Optional.empty();
            }
            List<double[]> inputsList = new ArrayList<>();
            List<Double> outputsList = new ArrayList<>();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                String line = reader.readLine(); // Skip header

                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length >= 5) {
                        double gridSize = Double.parseDouble(parts[0]);
                        double numBlueAgents = Double.parseDouble(parts[1]);
                        double numRedAgents = Double.parseDouble(parts[2]);
                        char flagPlacementTypeChar = parts[3].charAt(0);
                        double flagPlacementType = resolveFlagPlacementType(flagPlacementTypeChar);
                        double ninsts = Double.parseDouble(parts[4]);
                        inputsList.add(new double[]{gridSize, numBlueAgents, numRedAgents, flagPlacementType});
                        outputsList.add(ninsts);
                    }
                }
            }

            Points points = new Points();
            points.inputs = inputsList.toArray(new double[inputsList.size()][]);
            points.outputs = outputsList.stream().mapToDouble(Double::doubleValue).toArray();
            return Optional.of(points);

        } catch (IOException e) {
            return Optional.empty();
        }
    }
}

package pt.ulisboa.tecnico.cnv.resourcemanager.loadbalancer.parsers;

import java.io.*;
import java.util.*;
import java.util.Map;
import java.util.Optional;

public class RegressionPuzzleParser implements RegressionDataParser{

    private final String DEFAULT_FILE = "fifteenpuzzle_metrics.csv";

    public Optional<Points> parseMap(Map<String, String> workload) {
        //TODO
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
                    if (parts.length >= 3) {
                        double size = Double.parseDouble(parts[0]);
                        double shuffles = Double.parseDouble(parts[1]);
                        double ninsts = Double.parseDouble(parts[2]);

                        inputsList.add(new double[]{size, shuffles});
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

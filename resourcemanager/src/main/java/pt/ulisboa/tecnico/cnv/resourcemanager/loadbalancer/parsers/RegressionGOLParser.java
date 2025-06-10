package pt.ulisboa.tecnico.cnv.resourcemanager.loadbalancer.parsers;

import java.io.*;
import java.util.*;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class RegressionGOLParser implements RegressionDataParser {

    public final String DEFAULT_FILE = "gameoflife_results_with_metrics.csv";

    public Integer extractSize(String filename) {
        Pattern FILENAME_PATTERN = Pattern.compile("glider-(\\d+)-\\d+\\.json");
        Matcher matcher = FILENAME_PATTERN.matcher(filename);
        if (matcher.matches()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 0;
    }

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
                        String mapFilename = parts[0];
                        double size = extractSize(mapFilename);
                        double iterations = Double.parseDouble(parts[1]);
                        double ninsts = Double.parseDouble(parts[2]);

                        inputsList.add(new double[]{size, iterations});
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

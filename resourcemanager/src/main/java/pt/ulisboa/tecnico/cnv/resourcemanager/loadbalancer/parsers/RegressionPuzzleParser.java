package pt.ulisboa.tecnico.cnv.resourcemanager.loadbalancer.parsers;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import java.io.*;
import java.util.*;
import java.util.Map;
import java.util.Optional;

public class RegressionPuzzleParser implements RegressionDataParser {

  private final String DEFAULT_FILE = "fifteenpuzzle_metrics.csv";

  public Optional<Points> parseDB(List<Map<String, AttributeValue>> dbRecords) {
    double[][] records =
        dbRecords.stream()
            .map(
                item ->
                    new double[] {
                      Double.parseDouble(item.get("Size").getN()),
                      Double.parseDouble(item.get("Shuffles").getN()),
                      Double.parseDouble(item.get("Cost").getN()),
                    })
            .toArray(double[][]::new);
    double[][] inputs = new double[records.length][2];
    double[] outputs = new double[records.length];
    for (int i = 0; i < records.length; i++) {
      inputs[i][0] = records[i][0];
      inputs[i][1] = records[i][1];
      outputs[i] = records[i][2];
    }
    return Optional.of(new Points(inputs, outputs));
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

            inputsList.add(new double[] {size, shuffles});
            outputsList.add(ninsts);
          }
        }
      }

      double[][] inputs = inputsList.toArray(new double[inputsList.size()][]);
      double[] outputs = outputsList.stream().mapToDouble(Double::doubleValue).toArray();
      return Optional.of(new Points(inputs, outputs));

    } catch (IOException e) {
      return Optional.empty();
    }
  }
}

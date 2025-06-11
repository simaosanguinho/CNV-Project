package pt.ulisboa.tecnico.cnv.resourcemanager.loadbalancer.parsers;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import java.io.*;
import java.util.*;
import java.util.Map;
import java.util.Optional;

public class RegressionCTFParser implements RegressionDataParser {

  private final String DEFAULT_FILE = "ctf_results_with_metrics.csv";

  public Double resolveFlagPlacementType(char type) {
    return type == 'A' ? 1d : (type == 'B' ? 2d : 3d);
  }

  public Optional<Points> parseDB(List<Map<String, AttributeValue>> dbRecords) {
    double[][] records =
        dbRecords.stream()
            .map(
                record -> {
                  double gridSize = Double.parseDouble(record.get("GridSize").getN());
                  double numBlueAgents = Double.parseDouble(record.get("NumBlueAgents").getN());
                  double numRedAgents = Double.parseDouble(record.get("NumRedAgents").getN());
                  double flagPlacementType =
                      this.resolveFlagPlacementType(
                          record.get("FlagPlacementType").getS().charAt(0));
                  double cost = Double.parseDouble(record.get("Cost").getN());
                  return new double[] {
                    gridSize, numBlueAgents, numRedAgents, flagPlacementType, cost
                  };
                })
            .toArray(double[][]::new);
    double[][] inputs = new double[records.length][4];
    double[] outputs = new double[records.length];
    for (int i = 0; i < records.length; i++) {
      inputs[i][0] = records[i][0];
      inputs[i][1] = records[i][1];
      inputs[i][2] = records[i][2];
      inputs[i][3] = records[i][3];
      outputs[i] = records[i][4];
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
          if (parts.length >= 5) {
            double gridSize = Double.parseDouble(parts[0]);
            double numBlueAgents = Double.parseDouble(parts[1]);
            double numRedAgents = Double.parseDouble(parts[2]);
            char flagPlacementTypeChar = parts[3].charAt(0);
            double flagPlacementType = resolveFlagPlacementType(flagPlacementTypeChar);
            double ninsts = Double.parseDouble(parts[4]);
            inputsList.add(new double[] {gridSize, numBlueAgents, numRedAgents, flagPlacementType});
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

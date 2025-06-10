package pt.ulisboa.tecnico.cnv.resourcemanager.loadbalancer.parsers;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface RegressionDataParser {
    class Points {
        private final double[][] inputs;
        private final double[] outputs;
        Points(double[][] inputs, double[] outputs) {
            this.inputs = inputs;
            this.outputs = outputs;
        }
        public double[] getOutputs() {
            return outputs;
        }
        public double[][] getInputs() {
            return inputs;
        }
    }
    Optional<Points> parseDB(List<Map<String, AttributeValue>> dbRecords);
    Optional<Points> parseDefault();
}

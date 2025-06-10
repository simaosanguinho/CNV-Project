package pt.ulisboa.tecnico.cnv.resourcemanager.loadbalancer.estimators;

import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;
import pt.ulisboa.tecnico.cnv.resourcemanager.loadbalancer.parsers.RegressionDataParser;

import java.util.Optional;

public class PolynomialRegression {
    private final OLSMultipleLinearRegression regression;
    private final double[] regressionWeights;

    public PolynomialRegression(double[][] inputs, double[] outputs) {
        this.regression = new OLSMultipleLinearRegression();
        this.regression.newSampleData(outputs, inputs);
        this.regressionWeights = regression.estimateRegressionParameters();
    }

    public PolynomialRegression(RegressionDataParser parser) {
        Optional<RegressionDataParser.Points> points = parser.parseDefault();
        if (points.isEmpty()) {
            throw new IllegalArgumentException("No points found");
        }
        this.regression = new OLSMultipleLinearRegression();
        this.regression.newSampleData(points.get().getOutputs(), points.get().getInputs());
        this.regressionWeights = regression.estimateRegressionParameters();
    }

    public double estimate(double[] input) {
        double prediction = this.regressionWeights[0];
        for (int i = 1; i < this.regressionWeights.length; i++) {
            prediction += this.regressionWeights[i] * input[i - 1];
        }
        return prediction;
    }
}


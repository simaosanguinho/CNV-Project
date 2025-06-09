package pt.ulisboa.tecnico.cnv.resourcemanager.loadbalancer.estimators;

import java.util.List;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;

public class PolynomialRegression {
    private final double[][] inputs;
    private final double[] outputs;
    private final OLSMultipleLinearRegression regression;
    private final double[] regressionWeights;

    public PolynomialRegression(double[][] inputs, double[] outputs) {
        this.inputs = inputs;
        this.outputs = outputs;
        this.regression = new OLSMultipleLinearRegression();
        this.regression.newSampleData(this.outputs, this.inputs);
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


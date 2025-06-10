package pt.ulisboa.tecnico.cnv.resourcemanager.loadbalancer.estimators;

import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;
import pt.ulisboa.tecnico.cnv.resourcemanager.loadbalancer.parsers.RegressionDataParser;
import java.util.Optional;

import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;

public class PolynomialRegression {
    private final OLSMultipleLinearRegression regression;
    private final double[] regressionWeights;
    // Fixed to degree 2
    private final int degree = 2;
    private final int numFeatures;

    /* public PolynomialRegression(double[][] inputs, double[] outputs) {
        this.numFeatures = inputs[0].length;
        
        // Transform inputs to polynomial features
        double[][] polynomialInputs = transformToPolynomial(inputs);

        this.regression = new OLSMultipleLinearRegression();
        this.regression.newSampleData(outputs, polynomialInputs);
        this.regressionWeights = regression.estimateRegressionParameters();
    } */

    public PolynomialRegression(RegressionDataParser parser) {
        Optional<RegressionDataParser.Points> points = parser.parseDefault();
        if (points.isEmpty()) {
            throw new IllegalArgumentException("No points found");
        }

        this.numFeatures = points.get().getInputs()[0].length;

        // Transform inputs to polynomial features
        double[][] polynomialInputs = transformFeatures(points.get().getInputs());

        this.regression = new OLSMultipleLinearRegression();
        this.regression.newSampleData(points.get().getOutputs(), polynomialInputs);
        this.regressionWeights = regression.estimateRegressionParameters();
    }



    public PolynomialRegression(double[][] inputs, double[] outputs) {
        // Transform to polynomial + interaction terms
        this.numFeatures = inputs[0].length;
        double[][] transformedInputs = transformFeatures(inputs);

        this.regression = new OLSMultipleLinearRegression();
        this.regression.setNoIntercept(false);
        this.regression.newSampleData(outputs, transformedInputs);
        this.regressionWeights = regression.estimateRegressionParameters();
    }

    private double[][] transformFeatures(double[][] inputs) {
        int numSamples = inputs.length;
        int numFeatures = inputs[0].length;
        double[][] result = new double[numSamples][5]; // x1, x2, x1^2, x2^2, x1*x2

        for (int i = 0; i < numSamples; i++) {
            double x1 = inputs[i][0];
            double x2 = inputs[i][1];
            result[i][0] = x1;
            result[i][1] = x2;
            result[i][2] = x1 * x1;
            result[i][3] = x2 * x2;
            result[i][4] = x1 * x2;
        }

        return result;
    }

    public double estimate(double[] input) {
        double x1 = input[0];
        double x2 = input[1];
        double[] features = new double[]{
            x1,
            x2,
            x1 * x1,
            x2 * x2,
            x1 * x2
        };

        double prediction = regressionWeights[0]; // Intercept
        for (int i = 0; i < features.length; i++) {
            prediction += regressionWeights[i + 1] * features[i];
        }

        return prediction;
    }
}

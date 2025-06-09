package pt.ulisboa.tecnico.cnv.resourcemanager.loadbalancer.estimators;

import java.util.Optional;

public class FifteenPuzzleEstimator {

    private PolynomialRegression estimationFunction;
    private int requestCount = 0;
    private final int REQUEST_LIMIT = 40; // limit to 40 requests until the model is trained again

    public double estimateCost(int size, int shuffles) {
        Optional<Double> estimatedCost = checkDatabase(size, shuffles);
        if (estimatedCost.isPresent()) {
            return estimatedCost.get();
        }

        if (requestCount >= REQUEST_LIMIT) {
            // TODO ->  need te return type from the mss
            getLastRecordsFromDB();
            double[][] inputs = null;
            double[] outputs = null;
            this.estimationFunction = new PolynomialRegression(inputs, outputs);
        }

        double[] inputFeatures = new double[] {
            size,
            shuffles
        };
        requestCount++;
        return estimationFunction.estimate(inputFeatures);
    }

    public Optional<Double> checkDatabase(int size, int shuffles) {
        // Check if the request is already in the database
        // If it is, return the estimated cost
        // If not, add the request to the database and return null
        //TODO
        return Optional.empty();
    }

    public void getLastRecordsFromDB() {
        // TODO -> this will not return void
    }
}

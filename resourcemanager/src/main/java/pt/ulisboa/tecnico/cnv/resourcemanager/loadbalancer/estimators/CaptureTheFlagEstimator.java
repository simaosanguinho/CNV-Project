package pt.ulisboa.tecnico.cnv.resourcemanager.loadbalancer.estimators;

import java.util.Optional;

public class CaptureTheFlagEstimator {

    private PolynomialRegression estimationFunction;
    private int requestCount = 0;
    private final int REQUEST_LIMIT = 40; // limit to 40 requests until the model is trained again

    public double estimateCost(int gridSize, int numBlueAgents, int numRedAgents, char flagPlacementType) {
        Optional<Double> estimatedCost = checkDatabase(gridSize, numBlueAgents, numRedAgents, flagPlacementType);
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
            gridSize,
            numBlueAgents,
            numRedAgents,
            flagPlacementType == 'A' ? 1 : (flagPlacementType == 'B' ? 2 : 3)
        };
        requestCount++;
        return estimationFunction.estimate(inputFeatures);
    }

    public Optional<Double> checkDatabase(int gridSize, int numBlueAgents, int numRedAgents, char flagPlacementType) {
        // Check if the request is already in the database
        // If it is, return the estimated cost
        // If not, add the request to the database and return null
        //TODO
        return Optional.empty();
    }

    public void getLastRecordsFromDB() {
        // TODO
    }

}

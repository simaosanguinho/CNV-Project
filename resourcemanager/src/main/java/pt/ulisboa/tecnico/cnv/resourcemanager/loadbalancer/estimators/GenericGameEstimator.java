package pt.ulisboa.tecnico.cnv.resourcemanager.loadbalancer.estimators;

import java.util.Map;
import java.util.function.Function;

public abstract class GenericGameEstimator {
    protected String scriptPath;
    protected Function<Map<String, String>, Double> estimationFunction;

    abstract public double estimate(Map<String, String> workload);
    abstract public void updateEstimationFunction();
}

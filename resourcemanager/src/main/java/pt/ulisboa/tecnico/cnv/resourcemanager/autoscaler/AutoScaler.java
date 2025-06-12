package pt.ulisboa.tecnico.cnv.resourcemanager.autoscaler;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.cnv.resourcemanager.common.Instance;
import pt.ulisboa.tecnico.cnv.resourcemanager.common.InstancePool;

public class AutoScaler implements Runnable {
  private static final Logger logger = Logger.getLogger(AutoScaler.class.getName());

  // Thresholds
  private static final double HIGH_CPU_THRESHOLD = 80.0; // 80%
  private static final double LOW_CPU_THRESHOLD = 20.0; // 20%
  public static final double INDIVIDUAL_CPU_THRESHOLD = 80.0;

  // Monitoring interval
  private static final long MONITORING_INTERVAL_SECONDS = 30; // Check every 90 seconds

  private final InstancePool instancePool;
  private volatile boolean running = true;

  public AutoScaler(InstancePool instancePool) {
    this.instancePool = instancePool;
  }

  @Override
  public void run() {
    logger.info("AutoScaler started");

    // Create first instance if none exist
    if (instancePool.getAllInstances().isEmpty()) {
      Instance initialInstance = instancePool.createNewInstance();
      if (initialInstance != null) {
        logger.info("Created initial instance: " +
            initialInstance.getInstanceId());
      } else {
        logger.warning("Failed to create initial instance");
      }
    }

    // sleep for 5 minutes to allow everything to stabilize
    /* try {
      // Thread.sleep(TimeUnit.MINUTES.toMillis(5));
    } catch (InterruptedException e) {
      logger.warning("Initialization sleep interrupted");
      Thread.currentThread().interrupt();
    } */
    logger.info("Starting auto-scaling checks every " +
        MONITORING_INTERVAL_SECONDS + " seconds");

    while (running) {
      try {
        performAutoScalingCheck();
        Thread.sleep(TimeUnit.SECONDS.toMillis(MONITORING_INTERVAL_SECONDS));
      } catch (InterruptedException e) {
        logger.info("AutoScaler interrupted, stopping...");
        Thread.currentThread().interrupt();
        break;
      } catch (Exception e) {
        logger.severe("Error in AutoScaler: " + e.getMessage());
        e.printStackTrace();
      }
    }

    logger.info("AutoScaler stopped");
  }

  private void markOverloaded() {
    List<Instance> runningInstances = instancePool.getRunningInstances();
    for (Instance instance : runningInstances) {
      // instances that have high cpu usage are marked as overloaded
      // to avoid excessive requests
      if (instance.getLastCpuUtilization() >= AutoScaler.INDIVIDUAL_CPU_THRESHOLD) {
        instance.setState(Instance.InstanceState.OVERLOADED);
      }
    }
  }

  private void performAutoScalingCheck() {
    logger.fine("Performing auto-scaling check...");

    // Update instance states first
    instancePool.updateInstanceStates();

    // Get current running instances
    List<Instance> runningInstances = instancePool.getRunningInstances();
    List<Instance> overloadedInstances = instancePool.getOverloadedInstances();
    List<Instance> workingInstances = new ArrayList<>(runningInstances);
    workingInstances.addAll(overloadedInstances);

    if (runningInstances.isEmpty()) {
      logger.warning("No running instances found");
      return;
    }

    // Calculate median CPU usage
    double medianCpuUsage = calculateMedianCpuUsage(workingInstances); // this updates the instance classes cpu values with the new values
    logger.info(String.format("Median CPU usage: %.2f%%", medianCpuUsage));

    // Scale up if median CPU is above high threshold
    if (medianCpuUsage >= HIGH_CPU_THRESHOLD && instancePool.canScaleUp()) {
      logger.info(String.format(
          "Scaling up - Median CPU (%.2f%%) >= threshold (%.2f%%)",
          medianCpuUsage, HIGH_CPU_THRESHOLD));
      createNewInstance();
    }
    // Scale down if median CPU is below low threshold
    else if (medianCpuUsage <= LOW_CPU_THRESHOLD &&
        instancePool.canScaleDown()) {
      logger.info(String.format(
          "Scaling down - Median CPU (%.2f%%) <= threshold (%.2f%%)",
          medianCpuUsage, LOW_CPU_THRESHOLD));
      terminateInstance();
    } else {
      logger.fine("No scaling action needed");
    }

    // after updating machines cpus mark the ones with excessive cpu as overloaded
    // to avoid further requests
    markOverloaded();

    // Log current status periodically
    if (System.currentTimeMillis() % (5 * 60 * 1000) < MONITORING_INTERVAL_SECONDS * 1000) {
      instancePool.printStatus();
    }
  }

  private double calculateMedianCpuUsage(List<Instance> runningInstances) {
    // Get CPU utilization for all running instances
    List<Double> cpuValues = runningInstances.stream()
        .map(instance -> instancePool.getCpuUtilization(instance.getInstanceId()))
        .sorted()
        .collect(Collectors.toList());

    if (cpuValues.isEmpty()) {
      return 0.0;
    }

    int size = cpuValues.size();
    if (size % 2 == 0) {
      // Even number of instances - average of two middle values
      return (cpuValues.get(size / 2 - 1) + cpuValues.get(size / 2)) / 2.0;
    } else {
      // Odd number of instances - middle value
      return cpuValues.get(size / 2);
    }
  }

  private void createNewInstance() {
    Instance newInstance = instancePool.createNewInstance();
    if (newInstance != null) {
      logger.info("Successfully created new instance: " +
          newInstance.getInstanceId());
    } else {
      logger.warning("Failed to create new instance");
    }
  }

  private void terminateInstance() {

    if (instancePool.getRunningInstanceCount() <= 1) {
      logger.warning("Cannot terminate instance - only one running instance available");
      return;
    }
    // Find an instance that can be terminated
    List<Instance> runningInstances = instancePool.getRunningInstances();

    Instance instanceToTerminate = runningInstances.get(0);
    // set marked for termination
    instanceToTerminate.setMarkedForTermination(true);
    // Terminate the instance
    boolean terminated = instancePool.terminateInstance(instanceToTerminate.getInstanceId());
    // if the instance didnt terminate, try more 3 times with 10 seconds delay
    if (terminated) {
      logger.info("Successfully terminated instance: " + instanceToTerminate.getInstanceId());
    } else {
      logger.warning("Failed to terminate instance: " + instanceToTerminate.getInstanceId() +
          ". Retrying...");
      for (int i = 0; i < 3; i++) {
        try {
          Thread.sleep(10000); // wait 10 seconds before retrying
          terminated = instancePool.terminateInstance(instanceToTerminate.getInstanceId());
          if (terminated) {
            logger.info("Successfully terminated instance after retry: " +
                instanceToTerminate.getInstanceId());
            break;
          }
        } catch (InterruptedException e) {
          logger.warning("Termination retry interrupted");
          Thread.currentThread().interrupt();
          break;
        }
      }
      if (!terminated) {
        logger.severe("Failed to terminate instance after retries: " +
            instanceToTerminate.getInstanceId());
      }
    }
  }

  public void stop() {
    logger.info("Stopping AutoScaler...");
    running = false;
  }

  public boolean isRunning() {
    return running;
  }

  

  // Method to get metrics for monitoring/debugging
  public AutoScalerMetrics getMetrics() {
    List<Instance> allInstances = instancePool.getAllInstances();
    List<Instance> runningInstances = instancePool.getRunningInstances();

    double medianCpuUtilization = runningInstances.isEmpty() ? 0.0
        : calculateMedianCpuUsage(runningInstances);

    return new AutoScalerMetrics(allInstances.size(), runningInstances.size(),
        medianCpuUtilization);
  }

  public static class AutoScalerMetrics {
    public final int totalInstances;
    public final int runningInstances;
    public final double medianCpuUtilization;

    public AutoScalerMetrics(int totalInstances, int runningInstances,
        double medianCpuUtilization) {
      this.totalInstances = totalInstances;
      this.runningInstances = runningInstances;
      this.medianCpuUtilization = medianCpuUtilization;
    
    }

    @Override
    public String toString() {
      return String.format(
          "AutoScalerMetrics[total=%d, running=%d, medianCpu=%.2f%%]",
          totalInstances, runningInstances, medianCpuUtilization);
    }
  }
}

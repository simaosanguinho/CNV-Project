package pt.ulisboa.tecnico.cnv.resourcemanager.autoscaler;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import pt.ulisboa.tecnico.cnv.resourcemanager.common.Instance;
import pt.ulisboa.tecnico.cnv.resourcemanager.common.InstancePool;

public class AutoScaler implements Runnable {
    private static final Logger logger = Logger.getLogger(AutoScaler.class.getName());

    // Thresholds
    private static final double HIGH_CPU_THRESHOLD = 80.0; // 80%
    private static final double LOW_CPU_THRESHOLD = 20.0; // 20%
    private static final long THRESHOLD_DURATION_MINUTES = 1; // 1 minute

    // Monitoring interval
    private static final long MONITORING_INTERVAL_SECONDS = 30; // Check every 30 seconds

    private final InstancePool instancePool;
    private volatile boolean running = true;

    public AutoScaler(InstancePool instancePool) {
        this.instancePool = instancePool;
    }

    @Override
    public void run() {
        logger.info("AutoScaler started");

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

    private void performAutoScalingCheck() {
        logger.fine("Performing auto-scaling check...");

        // Update instance states first
        instancePool.updateInstanceStates();

        // Get current running instances
        List<Instance> runningInstances = instancePool.getRunningInstances();

        if (runningInstances.isEmpty()) {
            logger.warning("No running instances found");
            return;
        }

        // Check CPU utilization for each instance
        for (Instance instance : runningInstances) {
            checkInstanceCpuUtilization(instance);
        }

        // Check if we need to scale up
        checkScaleUp(runningInstances);

        // Check if we can terminate instances marked for termination
        checkTerminateInstances();

        // Log current status periodically
        if (System.currentTimeMillis() % (5 * 60 * 1000) < MONITORING_INTERVAL_SECONDS * 1000) {
            instancePool.printStatus();
        }
    }

    private void checkInstanceCpuUtilization(Instance instance) {
        double cpuUtilization = instancePool.getCpuUtilization(instance.getInstanceId());
        LocalDateTime now = LocalDateTime.now();

        // Update instance CPU data
        instance.setLastCpuUtilization(cpuUtilization);
        instance.setLastCpuCheckTime(now);

        logger.fine(String.format("Instance %s CPU: %.2f%%",
                instance.getInstanceId(), cpuUtilization));

        // Check for high CPU usage
        if (cpuUtilization >= HIGH_CPU_THRESHOLD) {
            handleHighCpuUsage(instance, now);
        } else {
            // Reset high CPU tracking if CPU drops below threshold
            instance.setHighCpuStartTime(null);
        }

        // Check for low CPU usage (only if not already marked for termination)
        if (!instance.isMarkedForTermination()) {
            if (cpuUtilization <= LOW_CPU_THRESHOLD) {
                handleLowCpuUsage(instance, now);
            } else {
                // Reset low CPU tracking if CPU rises above threshold
                instance.setLowCpuStartTime(null);
            }
        }
    }

    private void handleHighCpuUsage(Instance instance, LocalDateTime now) {
        if (instance.getHighCpuStartTime() == null) {
            // First time seeing high CPU
            instance.setHighCpuStartTime(now);
            logger.info(String.format("Instance %s started high CPU period at %.2f%%",
                    instance.getInstanceId(),
                    instance.getLastCpuUtilization()));
        } else {
            // Check if high CPU has persisted for the threshold duration
            long minutesHighCpu = ChronoUnit.MINUTES.between(instance.getHighCpuStartTime(), now);
            if (minutesHighCpu >= THRESHOLD_DURATION_MINUTES) {
                logger.warning(
                        String.format("Instance %s has high CPU (%.2f%%) for %d minutes",
                                instance.getInstanceId(),
                                instance.getLastCpuUtilization(), minutesHighCpu));

                // Trigger scale up if possible
                triggerScaleUp("High CPU detected");

                // Reset the timer to avoid constant scaling attempts
                instance.setHighCpuStartTime(now);
            }
        }
    }

    private void handleLowCpuUsage(Instance instance, LocalDateTime now) {
        if (instance.getLowCpuStartTime() == null) {
            // First time seeing low CPU
            instance.setLowCpuStartTime(now);
            logger.fine(String.format("Instance %s started low CPU period at %.2f%%",
                    instance.getInstanceId(),
                    instance.getLastCpuUtilization()));
        } else {
            // Check if low CPU has persisted for the threshold duration
            long minutesLowCpu = ChronoUnit.MINUTES.between(instance.getLowCpuStartTime(), now);
            if (minutesLowCpu >= THRESHOLD_DURATION_MINUTES) {
                logger.info(String.format(
                        "Instance %s has low CPU (%.2f%%) for %d minutes - marking for termination",
                        instance.getInstanceId(), instance.getLastCpuUtilization(),
                        minutesLowCpu));

                // Mark for termination if we can scale down
                if (instancePool.canScaleDown()) {
                    instance.setMarkedForTermination(true);
                    logger.info("Marked instance for termination: " +
                            instance.getInstanceId());
                } else {
                    logger.info(
                            "Cannot mark instance for termination - minimum instances reached");
                }

                // Reset the timer
                instance.setLowCpuStartTime(null);
            }
        }
    }

    private void checkScaleUp(List<Instance> runningInstances) {
        // Check if any instance has sustained high CPU and we can scale up
        boolean needsScaling = runningInstances.stream().anyMatch(
                instance -> instance.getLastCpuUtilization() >= HIGH_CPU_THRESHOLD);

        if (needsScaling && instancePool.canScaleUp()) {
            // Additional check: ensure we're not scaling too aggressively
            long healthyInstances = runningInstances.stream()
                    .filter(instance -> !instance.isMarkedForTermination())
                    .count();

            if (healthyInstances > 0) {
                double avgCpuUtilization = runningInstances.stream()
                        .filter(instance -> !instance.isMarkedForTermination())
                        .mapToDouble(Instance::getLastCpuUtilization)
                        .average()
                        .orElse(0.0);

                if (avgCpuUtilization >= HIGH_CPU_THRESHOLD * 0.8) { // 64% average
                    triggerScaleUp("Average CPU utilization high");
                }
            }
        }
    }

    private void triggerScaleUp(String reason) {
        logger.info("Triggering scale up: " + reason);
        Instance newInstance = instancePool.createNewInstance();
        if (newInstance != null) {
            logger.info("Successfully created new instance: " +
                    newInstance.getInstanceId());
        } else {
            logger.warning("Failed to create new instance");
        }
    }

    private void checkTerminateInstances() {
        List<Instance> instancesToTerminate = instancePool.getInstancesMarkedForTermination()
                .stream()
                .filter(Instance::canBeTerminated)
                .toList();

        for (Instance instance : instancesToTerminate) {
            logger.info("Terminating instance with no pending jobs: " +
                    instance.getInstanceId());
            instancePool.terminateInstance(instance.getInstanceId());
        }
    }

    public void stop() {
        logger.info("Stopping AutoScaler...");
        running = false;
    }

    public boolean isRunning() {
        return running;
    }

    // Helper methods for integration with load balancer
    public void onRequestReceived(String instanceId) {
        Instance instance = instancePool.getInstance(instanceId);
        if (instance != null) {
            instance.incrementPendingJobs();
        }
    }

    public void onRequestCompleted(String instanceId) {
        Instance instance = instancePool.getInstance(instanceId);
        if (instance != null) {
            instance.decrementPendingJobs();
        }
    }

    // Method to get metrics for monitoring/debugging
    public AutoScalerMetrics getMetrics() {
        List<Instance> allInstances = instancePool.getAllInstances();
        List<Instance> runningInstances = instancePool.getRunningInstances();

        double avgCpuUtilization = runningInstances.stream()
                .mapToDouble(Instance::getLastCpuUtilization)
                .average()
                .orElse(0.0);

        int totalPendingJobs = allInstances.stream().mapToInt(Instance::getPendingJobs).sum();

        return new AutoScalerMetrics(
                allInstances.size(), runningInstances.size(),
                instancePool.getInstancesMarkedForTermination().size(),
                avgCpuUtilization, totalPendingJobs);
    }

    public static class AutoScalerMetrics {
        public final int totalInstances;
        public final int runningInstances;
        public final int instancesMarkedForTermination;
        public final double averageCpuUtilization;
        public final int totalPendingJobs;

        public AutoScalerMetrics(int totalInstances, int runningInstances,
                int instancesMarkedForTermination,
                double averageCpuUtilization,
                int totalPendingJobs) {
            this.totalInstances = totalInstances;
            this.runningInstances = runningInstances;
            this.instancesMarkedForTermination = instancesMarkedForTermination;
            this.averageCpuUtilization = averageCpuUtilization;
            this.totalPendingJobs = totalPendingJobs;
        }

        @Override
        public String toString() {
            return String.format(
                    "AutoScalerMetrics[total=%d, running=%d, marked=%d, avgCpu=%.2f%%, jobs=%d]",
                    totalInstances, runningInstances, instancesMarkedForTermination,
                    averageCpuUtilization, totalPendingJobs);
        }
    }
}
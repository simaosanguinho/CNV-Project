package pt.ulisboa.tecnico.cnv.resourcemanager.common;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.*;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

public class InstancePool {
    private static final Logger logger = Logger.getLogger(InstancePool.class.getName());

    private final Map<String, Instance> instances = new ConcurrentHashMap<>();
    private final Ec2Client ec2Client;
    private final CloudWatchClient cloudWatchClient;
    private final String amiId;
    private final String instanceType;
    private final String keyName;
    private final List<String> securityGroupIds;
    private final String subnetId;
    private final int minInstances;
    private final int maxInstances;

    public InstancePool(Ec2Client ec2Client, CloudWatchClient cloudWatchClient,
                        String amiId, String instanceType, String keyName,
                        List<String> securityGroupIds, String subnetId,
                        int minInstances, int maxInstances) {
        this.ec2Client = ec2Client;
        this.cloudWatchClient = cloudWatchClient;
        this.amiId = amiId;
        this.instanceType = instanceType;
        this.keyName = keyName;
        this.securityGroupIds = securityGroupIds;
        this.subnetId = subnetId;
        this.minInstances = minInstances;
        this.maxInstances = maxInstances;
    }

    public void addInstance(Instance instance) {
        instances.put(instance.getInstanceId(), instance);
        logger.info("Added instance to pool: " + instance.getInstanceId());
    }

    public void removeInstance(String instanceId) {
        Instance removed = instances.remove(instanceId);
        if (removed != null) {
            logger.info("Removed instance from pool: " + instanceId);
        }
    }

    public Instance getInstance(String instanceId) {
        return instances.get(instanceId);
    }

    public List<Instance> getAllInstances() {
        return new ArrayList<>(instances.values());
    }

    public List<Instance> getRunningInstances() {
        return instances.values()
                .stream()
                .filter(
                        instance -> instance.getState() == Instance.InstanceState.RUNNING)
                .toList();
    }

    public List<Instance> getInstancesMarkedForTermination() {
        return instances.values()
                .stream()
                .filter(Instance::isMarkedForTermination)
                .toList();
    }

    public int getRunningInstanceCount() {
        return getRunningInstances().size();
    }

    public boolean canScaleUp() {
        return getRunningInstanceCount() < maxInstances;
    }

    public boolean canScaleDown() {
        return getRunningInstanceCount() > minInstances;
    }

    public Instance createNewInstance() {
        if (!canScaleUp()) {
            logger.warning("Cannot scale up - maximum instances reached: " +
                    maxInstances);
            return null;
        }

        try {
            RunInstancesRequest runRequest = RunInstancesRequest.builder()
                    .imageId(amiId)
                    .instanceType(instanceType)
                    .keyName(keyName)
                    .securityGroupIds(securityGroupIds)
                    .subnetId(subnetId)
                    .minCount(1)
                    .maxCount(1)
                    .build();

            RunInstancesResponse response = ec2Client.runInstances(runRequest);
            software.amazon.awssdk.services.ec2.model.Instance ec2Instance = response.instances().get(0);

            Instance instance = new Instance(ec2Instance.instanceId(), ec2Instance.publicIpAddress(),
                    ec2Instance.privateIpAddress());

            addInstance(instance);
            logger.info("Created new instance: " + instance.getInstanceId());
            return instance;

        } catch (Exception e) {
            logger.severe("Failed to create new instance: " + e.getMessage());
            return null;
        }
    }

    public void terminateInstance(String instanceId) {
        Instance instance = getInstance(instanceId);
        if (instance == null) {
            logger.warning("Instance not found for termination: " + instanceId);
            return;
        }

        if (!instance.canBeTerminated()) {
            logger.warning("Cannot terminate instance - has pending jobs: " +
                    instanceId);
            return;
        }

        try {
            TerminateInstancesRequest terminateRequest = TerminateInstancesRequest.builder().instanceIds(instanceId).build();

            ec2Client.terminateInstances(terminateRequest);
            instance.setState(Instance.InstanceState.TERMINATED);
            removeInstance(instanceId);
            logger.info("Terminated instance: " + instanceId);

        } catch (Exception e) {
            logger.severe("Failed to terminate instance " + instanceId + ": " +
                    e.getMessage());
        }
    }

    public double getCpuUtilization(String instanceId) {
        try {
            Instant endTime = Instant.now();
            Instant startTime = endTime.minusSeconds(300); // Last 5 minutes

            GetMetricStatisticsRequest request = GetMetricStatisticsRequest.builder()
                    .namespace("AWS/EC2")
                    .metricName("CPUUtilization")
                    .dimensions(Dimension.builder()
                            .name("InstanceId")
                            .value(instanceId)
                            .build())
                    .startTime(startTime)
                    .endTime(endTime)
                    .period(60) // 1 minute intervals
                    .statistics(Statistic.AVERAGE)
                    .build();

            GetMetricStatisticsResponse response = cloudWatchClient.getMetricStatistics(request);

            if (response.datapoints().isEmpty()) {
                return 0.0; // No data available yet
            }

            // Get the most recent datapoint
            Optional<Datapoint> latestDatapoint = response.datapoints().stream().max(
                    Comparator.comparing(Datapoint::timestamp));

            return latestDatapoint.map(Datapoint::average).orElse(0.0);

        } catch (Exception e) {
            logger.severe("Failed to get CPU utilization for instance " + instanceId +
                    ": " + e.getMessage());
            return 0.0;
        }
    }

    public void updateInstanceStates() {
        if (instances.isEmpty()) {
            return;
        }

        try {
            DescribeInstancesRequest request = DescribeInstancesRequest.builder()
                    .instanceIds(instances.keySet())
                    .build();

            DescribeInstancesResponse response = ec2Client.describeInstances(request);

            for (Reservation reservation : response.reservations()) {
                for (software.amazon.awssdk.services.ec2.model.Instance ec2Instance : reservation.instances()) {
                    Instance instance = getInstance(ec2Instance.instanceId());
                    if (instance != null) {
                        // Update state
                        Instance.InstanceState newState = mapEc2StateToInstanceState(ec2Instance.state().name());
                        instance.setState(newState);

                        // Update IP addresses if they've changed
                        if (ec2Instance.publicIpAddress() != null) {
                            instance.setPublicIpAddress(ec2Instance.publicIpAddress());
                        }
                        if (ec2Instance.privateIpAddress() != null) {
                            instance.setPrivateIpAddress(ec2Instance.privateIpAddress());
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.severe("Failed to update instance states: " + e.getMessage());
        }
    }

    private Instance.InstanceState mapEc2StateToInstanceState(InstanceStateName ec2State) {
        if (ec2State == InstanceStateName.PENDING) {
            return Instance.InstanceState.PENDING;
        } else if (ec2State == InstanceStateName.RUNNING) {
            return Instance.InstanceState.RUNNING;
        } else if (ec2State == InstanceStateName.STOPPING) {
            return Instance.InstanceState.STOPPING;
        } else if (ec2State == InstanceStateName.STOPPED) {
            return Instance.InstanceState.STOPPED;
        } else if (ec2State == InstanceStateName.TERMINATED) {
            return Instance.InstanceState.TERMINATED;
        } else {
            return Instance.InstanceState.PENDING;
        }
    }

    public Optional<Instance> selectInstanceForRequest() {
        return getRunningInstances()
                .stream()
                .filter(instance -> !instance.isMarkedForTermination())
                .min(Comparator.comparingDouble(Instance::getAccumulatedComplexity));
    }

    public void printStatus() {
        logger.info("=== Instance Pool Status ===");
        logger.info("Total instances: " + instances.size());
        logger.info("Running instances: " + getRunningInstanceCount());
        logger.info("Instances marked for termination: " +
                getInstancesMarkedForTermination().size());

        for (Instance instance : instances.values()) {
            logger.info(instance.toString());
        }
        logger.info("===========================");
    }
}
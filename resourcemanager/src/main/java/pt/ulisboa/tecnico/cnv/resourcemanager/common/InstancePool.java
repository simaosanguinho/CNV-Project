package pt.ulisboa.tecnico.cnv.resourcemanager.common;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.Datapoint;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsRequest;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsResponse;
import software.amazon.awssdk.services.cloudwatch.model.Statistic;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.InstanceStateName;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.RunInstancesRequest;
import software.amazon.awssdk.services.ec2.model.RunInstancesResponse;
import software.amazon.awssdk.services.ec2.model.TerminateInstancesRequest;

public class InstancePool {
  private static final Logger logger = Logger.getLogger(InstancePool.class.getName());
  // Total observation time in milliseconds.
  private static long OBS_TIME = 1000 * 60 * 20;

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

  public InstancePool(
      Ec2Client ec2Client,
      CloudWatchClient cloudWatchClient,
      String amiId,
      String instanceType,
      String keyName,
      List<String> securityGroupIds,
      String subnetId,
      int minInstances,
      int maxInstances) {
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
    return instances.values().stream()
        .filter(instance -> instance.getState() == Instance.InstanceState.RUNNING)
        .collect(Collectors.toList());
  }

  public List<Instance> getPendingInstances() {
    return instances.values().stream()
            .filter(instance -> instance.getState() == Instance.InstanceState.PENDING)
            .collect(Collectors.toList());
  }

  public List<Instance> getOverloadedInstances() {
    return instances.values().stream()
            .filter(instance -> instance.getState() == Instance.InstanceState.OVERLOADED)
            .collect(Collectors.toList());
  }

  public int getOverloadedInstanceCount() {
    return getOverloadedInstances().size();
  }

  public int getPendingInstanceCount() {
    return getPendingInstances().size();
  }
  
  public List<Instance> getInstancesMarkedForTermination() {
    return instances.values().stream().filter(Instance::isMarkedForTermination).collect(Collectors.toList());
  }

    public int getRunningInstanceCount() {
        return getRunningInstances().size();
    }

    public int getTotalInstanceCount() {
        return instances.size();
    }

    public boolean canScaleUp() {
        return getTotalInstanceCount() < maxInstances;
    }

    public boolean canScaleDown() {
        return getTotalInstanceCount() > minInstances;
    }

  public Instance createNewInstance() {
    if (!canScaleUp()) {
      logger.warning("Cannot scale up - maximum instances reached: " + maxInstances);
      return null;
    }

    try {
      RunInstancesRequest runRequest =
          RunInstancesRequest.builder()
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

      Instance instance =
          new Instance(
              ec2Instance.instanceId(),
              ec2Instance.publicIpAddress(),
              ec2Instance.privateIpAddress());

      addInstance(instance);
      logger.info("Created new instance: " + instance.getInstanceId());
      return instance;

    } catch (Exception e) {
      logger.severe("Failed to create new instance: " + e.getMessage());
      return null;
    }
  }

    public boolean terminateInstance(String instanceId) {
      
        Instance instance = getInstance(instanceId);
        if (instance == null) {
            logger.warning("Instance not found for termination: " + instanceId);
            return false;
        }

        if (!instance.canBeTerminated()) {
            logger.warning("Cannot terminate instance - has pending jobs: " +
                    instanceId);
            return false;
        }

    try {
      TerminateInstancesRequest terminateRequest =
          TerminateInstancesRequest.builder().instanceIds(instanceId).build();

            ec2Client.terminateInstances(terminateRequest);
            instance.setState(Instance.InstanceState.TERMINATED);
            removeInstance(instanceId);
            logger.info("Terminated instance: " + instanceId);
            return true;

        } catch (Exception e) {
            logger.severe("Failed to terminate instance " + instanceId + ": " +
                    e.getMessage());
            return false;
        }
    }

  public double getCpuUtilization(String instanceId) {
    System.out.println("Getting CPU utilization for instance " + instanceId);
    try {
//      Instant endTime = Instant.now();
//      Instant startTime = endTime.minusSeconds(600); // Last 5 minutes

      Dimension instanceDimension =  Dimension.builder().name(instanceId).value(instanceId).build();
//      List<Dimension> dims = new ArrayList<Dimension>();
//      dims.add(instanceDimension);

      GetMetricStatisticsRequest request =
          GetMetricStatisticsRequest.builder()
              .namespace("AWS/EC2")
              .metricName("CPUUtilization")
              .dimensions(Dimension.builder().name("InstanceId").value(instanceId).build())
              .startTime((new Date(new Date().getTime() - OBS_TIME)).toInstant())
              .endTime(new Date().toInstant())
              .period(30) // 1 minute intervals
              .statistics(Statistic.AVERAGE)
              .build();

      GetMetricStatisticsResponse response = cloudWatchClient.getMetricStatistics(request);
      System.out.println("Got CPU utilization response: " + response.toString());

      if (response.datapoints().isEmpty()) {
        System.out.println("No datapoints available for instance " + instanceId);
        // update the cpu usage in the instance object
        //instances.get(instanceId).setLastCpuUtilization(0.0);
        return 0.0; // No data available yet
      }

      // Get the most recent datapoint
      Optional<Datapoint> latestDatapoint =
          response.datapoints().stream().max(Comparator.comparing(Datapoint::timestamp));
      System.out.println("Latest datapoint: " + latestDatapoint.toString());
      double usage = latestDatapoint.map(Datapoint::average).orElse(0.0);
      System.out.println("CPU utilization: " + usage);
      // update the cpu usage in the instance object
      instances.get(instanceId).setLastCpuUtilization(usage);

      return usage;

    } catch (Exception e) {
      logger.severe(
          "Failed to get CPU utilization for instance " + instanceId + ": " + e.getMessage());
      return 0.0;
    }
  }

  public void updateInstanceStates() {
    if (instances.isEmpty()) {
      return;
    }

    try {
      DescribeInstancesRequest request =
          DescribeInstancesRequest.builder().instanceIds(instances.keySet()).build();

      DescribeInstancesResponse response = ec2Client.describeInstances(request);

      for (Reservation reservation : response.reservations()) {
        for (software.amazon.awssdk.services.ec2.model.Instance ec2Instance :
            reservation.instances()) {
          Instance instance = getInstance(ec2Instance.instanceId());
          if (instance != null) {
            // Update state
            Instance.InstanceState newState =
                mapEc2StateToInstanceState(ec2Instance.state().name());
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
    Optional<Instance> result =  getRunningInstances().stream()
        .filter(instance -> !instance.isMarkedForTermination())
        .min(Comparator.comparingDouble(Instance::getAccumulatedComplexity));

    return result;
  }

  public void printStatus() {
    logger.info("=== Instance Pool Status ===");
    logger.info("Total instances: " + instances.size());
    logger.info("Running instances: " + getRunningInstanceCount());
    logger.info("Instances marked as overloaded" + getOverloadedInstanceCount());
    logger.info("Instances marked for termination: " + getInstancesMarkedForTermination().size());

    for (Instance instance : instances.values()) {
      /* if (instance.getName() != null) {
        // LAMBDA instances dont have public IP addresses
        continue;
      } */
      logger.info(instance.toString());
    }
    logger.info("===========================");
  }
}

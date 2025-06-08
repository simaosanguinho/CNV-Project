package pt.ulisboa.tecnico.cnv.resourcemanager.common;

import java.time.LocalDateTime;

public class Instance {
    private String instanceId;
    private String publicIpAddress;
    private String privateIpAddress;
    private InstanceState state;
    private LocalDateTime launchTime;
    private LocalDateTime lastCpuCheckTime;
    private double lastCpuUtilization;
    private LocalDateTime highCpuStartTime;
    private LocalDateTime lowCpuStartTime;
    private boolean markedForTermination;
    private int pendingJobs;

    public enum InstanceState {
        PENDING, RUNNING, STOPPING, STOPPED, TERMINATED
    }

    public Instance(String instanceId, String publicIpAddress,
            String privateIpAddress) {
        this.instanceId = instanceId;
        this.publicIpAddress = publicIpAddress;
        this.privateIpAddress = privateIpAddress;
        this.state = InstanceState.PENDING;
        this.launchTime = LocalDateTime.now();
        this.lastCpuUtilization = 0.0;
        this.markedForTermination = false;
        this.pendingJobs = 0;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getPublicIpAddress() {
        return publicIpAddress;
    }

    public void setPublicIpAddress(String publicIpAddress) {
        this.publicIpAddress = publicIpAddress;
    }

    public String getPrivateIpAddress() {
        return privateIpAddress;
    }

    public void setPrivateIpAddress(String privateIpAddress) {
        this.privateIpAddress = privateIpAddress;
    }

    public InstanceState getState() {
        return state;
    }

    public void setState(InstanceState state) {
        this.state = state;
    }

    public LocalDateTime getLaunchTime() {
        return launchTime;
    }

    public LocalDateTime getLastCpuCheckTime() {
        return lastCpuCheckTime;
    }

    public void setLastCpuCheckTime(LocalDateTime lastCpuCheckTime) {
        this.lastCpuCheckTime = lastCpuCheckTime;
    }

    public double getLastCpuUtilization() {
        return lastCpuUtilization;
    }

    public void setLastCpuUtilization(double lastCpuUtilization) {
        this.lastCpuUtilization = lastCpuUtilization;
    }

    public LocalDateTime getHighCpuStartTime() {
        return highCpuStartTime;
    }

    public void setHighCpuStartTime(LocalDateTime highCpuStartTime) {
        this.highCpuStartTime = highCpuStartTime;
    }

    public LocalDateTime getLowCpuStartTime() {
        return lowCpuStartTime;
    }

    public void setLowCpuStartTime(LocalDateTime lowCpuStartTime) {
        this.lowCpuStartTime = lowCpuStartTime;
    }

    public boolean isMarkedForTermination() {
        return markedForTermination;
    }

    public void setMarkedForTermination(boolean markedForTermination) {
        this.markedForTermination = markedForTermination;
    }

    public int getPendingJobs() {
        return pendingJobs;
    }

    public void setPendingJobs(int pendingJobs) {
        this.pendingJobs = pendingJobs;
    }

    public void incrementPendingJobs() {
        this.pendingJobs++;
    }

    public void decrementPendingJobs() {
        if (this.pendingJobs > 0) {
            this.pendingJobs--;
        }
    }

    public boolean canBeTerminated() {
        return markedForTermination && pendingJobs == 0;
    }

    @Override
    public String toString() {
        return String.format(
                "Instance[id=%s, ip=%s, state=%s, cpu=%.2f%%, jobs=%d, marked=%b]",
                instanceId, publicIpAddress, state, lastCpuUtilization, pendingJobs,
                markedForTermination);
    }
}
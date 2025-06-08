package pt.ulisboa.tecnico.cnv.resourcemanager;

import pt.ulisboa.tecnico.cnv.resourcemanager.autoscaler.AutoScaler;
import pt.ulisboa.tecnico.cnv.resourcemanager.common.InstancePool;
import pt.ulisboa.tecnico.cnv.resourcemanager.loadbalancer.LoadBalancer;

public class ResourceManager {
    public static void main(String[] args) {
        InstancePool instancePool = new InstancePool();
        Thread loadbalancer = new Thread(new LoadBalancer(instancePool));
        Thread autoscaler = new Thread(new AutoScaler(instancePool));
        loadbalancer.start();
        autoscaler.start();
    }
}

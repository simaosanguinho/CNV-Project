package pt.ulisboa.tecnico.cnv.resourcemanager.autoscaler;

import pt.ulisboa.tecnico.cnv.resourcemanager.common.InstancePool;

public class AutoScaler implements Runnable {

    InstancePool instancePool;

    public AutoScaler(InstancePool instancePool) {
        this.instancePool = instancePool;
    }

    @Override
    public void run() {
    }
}

package pt.ulisboa.tecnico.cnv.resourcemanager.loadbalancer;

import com.sun.net.httpserver.HttpServer;
import pt.ulisboa.tecnico.cnv.resourcemanager.common.InstancePool;
import pt.ulisboa.tecnico.cnv.resourcemanager.loadbalancer.handlers.RootLoadHandler;
import pt.ulisboa.tecnico.cnv.resourcemanager.loadbalancer.handlers.GameOfLifeLoadHandler;
import pt.ulisboa.tecnico.cnv.resourcemanager.loadbalancer.handlers.FifteenPuzzleLoadHandler;
import pt.ulisboa.tecnico.cnv.resourcemanager.loadbalancer.handlers.CaptureTheFlagLoadHandler;
import pt.ulisboa.tecnico.cnv.mss.MSS;

import java.io.IOException;
import java.net.InetSocketAddress;

public class LoadBalancer implements Runnable {

    InstancePool instancePool;
    MSS mss;

    public LoadBalancer(InstancePool instancePool, MSS mss) {
        this.mss = mss;
        this.instancePool = instancePool;
    }

    @Override
    public void run() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(8001), 0);
            server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
            server.createContext("/", new RootLoadHandler());
            server.createContext("/gameoflife", new GameOfLifeLoadHandler(instancePool, mss));
            server.createContext("/fifteenpuzzle", new FifteenPuzzleLoadHandler(instancePool, mss));
            server.createContext("/capturetheflag", new CaptureTheFlagLoadHandler(instancePool, mss));
            server.start();
            System.out.println("LoadBalancer started on port 8001");
        } catch (IOException e) {
            System.err.println("Failed to start LoadBalancer: " + e.getMessage());
        }
    }
}
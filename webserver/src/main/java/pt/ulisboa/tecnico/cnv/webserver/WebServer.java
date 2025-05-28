package pt.ulisboa.tecnico.cnv.webserver;

import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpServer;

import pt.ulisboa.tecnico.cnv.capturetheflag.CaptureTheFlagHandler;
import pt.ulisboa.tecnico.cnv.fifteenpuzzle.FifteenPuzzleHandler;
import pt.ulisboa.tecnico.cnv.gameoflife.GameOfLifeHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;

public class WebServer {
    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);

        // Create dirs for metrics
        Path metricsDir = Path.of("metrics");
        Path golDir = metricsDir.resolve("gol-metrics");
        Path fifteenPuzzleDir = metricsDir.resolve("fifteen-puzzle");
        Path ctfDir = metricsDir.resolve("ctf");
        Files.createDirectories(golDir);
        Files.createDirectories(fifteenPuzzleDir);
        Files.createDirectories(ctfDir);

        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        server.createContext("/", new RootHandler());
        server.createContext("/gameoflife", new GameOfLifeHandler(golDir));
        server.createContext("/fifteenpuzzle", new FifteenPuzzleHandler(fifteenPuzzleDir));
        server.createContext("/capturetheflag", new CaptureTheFlagHandler(ctfDir));
        server.start();
    }
}

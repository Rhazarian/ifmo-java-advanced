package ru.ifmo.rain.alekperov.udp;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

public class HelloServerImpl implements HelloServer {

    private static final int QUEUE_SIZE = 1 << 16;

    private DatagramSocket socket = null;
    private ExecutorService executor = null;
    private ExecutorService connectionsHandler = null;

    private void handleIOException(final IOException ex) {
        System.err.println("I/O error at hello server:");
        System.err.println(ex.getMessage());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(final int port, final int threads) {
        close();
        try {
            connectionsHandler = Executors.newSingleThreadExecutor();
            executor = new ThreadPoolExecutor(threads, threads, 30, TimeUnit.SECONDS,
                    new ArrayBlockingQueue<>(QUEUE_SIZE), new ThreadPoolExecutor.DiscardPolicy());
            socket = new DatagramSocket(port);
            final var bufferSize = socket.getReceiveBufferSize();
            connectionsHandler.submit(() -> { while (!socket.isClosed() && !Thread.currentThread().isInterrupted()) {
                try {
                    final var packet = Util.receivePacket(socket, bufferSize);
                    final var msg =  Util.getMessage(packet);
                    final var responseBytes = "Hello, ".concat(msg).getBytes(StandardCharsets.UTF_8);
                    packet.setData(responseBytes);
                    executor.submit(() -> {
                        try {
                            socket.send(packet);
                        } catch (final IOException ex) {
                            handleIOException(ex);
                        }
                    });
                } catch (final IOException ex) {
                    if (!socket.isClosed()) {
                        handleIOException(ex);
                    }
                }
            }});
        } catch (final IOException ex) {
            handleIOException(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        if (socket != null) {
            connectionsHandler.shutdownNow();
            executor.shutdownNow();
            socket.close();
        }
    }

}

package ru.ifmo.rain.alekperov.udp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HelloServerImpl implements HelloServer {

    private final DatagramChannel channel;
    private final ExecutorService executor;

    public void(final int port, final int threads) throws IOException {
        var selector = Selector.open();
        channel = DatagramChannel.open().bind(new InetSocketAddress(port));
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_READ);
        selector.selectNow();
        selector.select()
        serverChannel = AsynchronousServerSocketChannel.open(group).bind(new InetSocketAddress(port));
        serverChannel.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {
            public void completed(final AsynchronousSocketChannel channel, final Void attachment) {
                serverChannel.accept(null, this);
            }

            public void failed(final Throwable exception, final Void attachment) {

            }
        });
    }

    @Override
    public void close() throws IOException {
        channel.close();
        selector.close();
    }

}

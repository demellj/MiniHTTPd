package demellj.minihttpd;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class Client {
    private final SocketChannel channel;
    private final LineBuffer lineBuffer = new LineBuffer();
    private final ByteBuffer outBuffer = ByteBuffer.allocateDirect(1024);

    private final AtomicLong lastActivity = new AtomicLong(0);
    private final AtomicBoolean supportKeepAlive = new AtomicBoolean(false);
    private final AtomicLong numRequests = new AtomicLong(0);

    Client(SocketChannel channel) {
        this.channel = channel;
        updateActivity();
    }

    public LineBuffer getLineBuffer() {
        return lineBuffer;
    }

    public ByteBuffer getOutBuffer() {
        return outBuffer;
    }

    public void close() throws IOException {
        channel.close();
    }

    public boolean getSupportsKeepAlive() {
        return supportKeepAlive.get();
    }

    public void enableKeepAlive() {
        supportKeepAlive.set(true);
    }

    public void updateActivity() {
        numRequests.incrementAndGet();
        lastActivity.set(System.nanoTime()/1000000);
    }

    public long getLastActivity() {
        return lastActivity.get();
    }

    public long getNumRequests() {
        return numRequests.get();
    }
}

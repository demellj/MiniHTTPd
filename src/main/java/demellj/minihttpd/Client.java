package demellj.minihttpd;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class Client {
    private final SocketChannel channel;
    private final Buffers buffers = new Buffers();

    private final AtomicLong lastActivity = new AtomicLong(0);
    private final AtomicBoolean supportKeepAlive = new AtomicBoolean(false);
    private final AtomicLong numRequests = new AtomicLong(0);

    Client(SocketChannel channel) {
        this.channel = channel;
        updateActivity();
    }

    public Buffers getBuffers() {
        return buffers;
    }

    public void close() throws IOException {
        channel.close();
    }

    public SocketChannel getChannel() {
        return channel;
    }

    public void writeChars(CharBuffer charBuffer, CharsetEncoder encoder) throws IOException {
        final ByteBuffer byteBuffer = buffers.getByteBuffer();
        byteBuffer.clear();

        CoderResult result = null;

        encoder.reset();
        while (charBuffer.hasRemaining()) {
            result = encoder.encode(charBuffer, byteBuffer, false);
            if (result.isOverflow()) {
                byteBuffer.flip();
                channel.write(byteBuffer);
                byteBuffer.flip();
            } else if (result.isError()) {
                break;
            }
        }

        if (result != null && result.isUnderflow()) {
            result = encoder.encode(charBuffer, byteBuffer, true);
            encoder.flush(byteBuffer);
            byteBuffer.flip();
        }

        if (byteBuffer.hasRemaining()) channel.write(byteBuffer);
    }

    public boolean getSupportsKeepAlive() {
        return supportKeepAlive.get();
    }

    public void enableKeepAlive() {
        supportKeepAlive.set(true);
    }

    public void updateActivity() {
        numRequests.incrementAndGet();
        lastActivity.set(System.nanoTime() / 1000000);
    }

    public long getLastActivity() {
        return lastActivity.get();
    }

    public long getNumRequests() {
        return numRequests.get();
    }
}

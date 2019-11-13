package demellj.minihttpd;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class SessionCleanupWorker implements Runnable {
    private final ConcurrentHashMap<SocketChannel, Client> sessions;
    private final AtomicBoolean isRunning;

    public static final int KEEP_ALIVE_SEC = 10;
    public static final int MAX_PERSISTENT_REQ = 1000;

    public SessionCleanupWorker(ConcurrentHashMap<SocketChannel, Client> sessions,
                                AtomicBoolean isRunning) {
        this.sessions = sessions;
        this.isRunning = isRunning;
    }

    @Override
    public void run() {
        while (isRunning.get()) {
            for (final Map.Entry<SocketChannel, Client> pair : sessions.entrySet()) {
                final SocketChannel chan = pair.getKey();
                final Client client = pair.getValue();

                final long now = System.nanoTime() / 1000000;
                final long elapsed = now - client.getLastActivity();

                if (client.getSupportsKeepAlive() &&
                        (elapsed > KEEP_ALIVE_SEC * 1000 || client.getNumRequests() > MAX_PERSISTENT_REQ)) {
                    sessions.remove(chan);
                    try {
                        final String addr = chan.getRemoteAddress().toString();
                        client.close();
                        System.err.println(String.format("%s disconnected", addr));
                    } catch (ClosedChannelException cce) {
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            try {
                Thread.sleep(KEEP_ALIVE_SEC / 2);
            } catch (InterruptedException ignored) {
            }
        }
    }
}

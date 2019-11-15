package demellj.minihttpd;

import java.io.IOException;
import java.nio.channels.*;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

public class Worker implements Runnable {
    private final ConcurrentHashMap<SocketChannel, Client> sessions;
    private final AtomicBoolean isRunning;
    private final Selector selector;
    private final ThreadSafeResponder responder;
    private final WorkerSync sync;

    Worker(ConcurrentHashMap<SocketChannel, Client> sessions,
           AtomicBoolean isRunning,
           Selector selector,
           ThreadSafeResponder responder,
           WorkerSync sync) {
        this.sessions = sessions;
        this.isRunning = isRunning;
        this.selector = selector;
        this.responder = responder;
        this.sync = sync;
    }

    @Override
    public void run() {
        while (isRunning.get()) {
            try {
                SelectableChannel ch = null;

                sync.signalAndWait();

                // Synchronize on 'sync', simply because it is shared by all workers.
                // Attempting to ensure a consistent view of selectedKeys.
                synchronized (sync) {
                    if (selector.select() > 0) {
                        final Set<SelectionKey> selectedKeys = selector.selectedKeys();
                        for (final SelectionKey key : selectedKeys) {
                            boolean needsClosing = false;
                            try {
                                if (key.isValid() && key.isReadable()) {
                                    ch = key.channel();
                                } else {
                                    needsClosing = true;
                                }
                                selectedKeys.remove(key);
                            } catch (CancelledKeyException ignored) {
                                needsClosing = true;
                            }

                            if (needsClosing && key.channel() instanceof SocketChannel) {
                                final SocketChannel chan = (SocketChannel) key.channel();
                                final Client client = sessions.get(chan);
                                sessions.remove(chan, client);
                                chan.close();
                            }
                            break;
                        }
                    }
                }

                // Perform IO without blocking other threads
                if (ch instanceof SocketChannel)
                    performClientRead((SocketChannel) ch);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void performClientDisconnected(SocketChannel ch) {
        sessions.remove(ch);
        try {
            final String addr = ch.getRemoteAddress().toString();
            System.err.println(String.format("%s disconnected", addr));
            ch.close();
        } catch (ClosedChannelException ignored) {
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void performClientRead(SocketChannel chan) {
        final Client client = sessions.get(chan);

        if (client == null || !sessions.remove(chan, client))
            return;

        final LineBuffer lineBuffer = client.getBuffers().getLineBuffer();

        try {
            final String addr = chan.getRemoteAddress().toString();
            System.err.println(String.format("%s handled", addr));

            lineBuffer.read(chan);
            sessions.put(chan, client);
            lineBuffer.flush();

            boolean keepAlive = false;

            Request req = null;
            while ((req = Request.Parser.parse(lineBuffer)) != null) {
                final ResponseWriter writer = new ResponseWriter("HTTP/1.1", client);

                final String connection = req.headers.get("connection");
                keepAlive = connection != null && connection.toLowerCase().contains("keep-alive");

                writer.appendHeader("Server", "MiniHTTPd");

                if (keepAlive) {
                    client.updateActivity();
                    writer.appendHeader("Connection", "keep-alive");
                    writer.appendHeader("Keep-Alive", String.format("timeout=%d, max=%d",
                            SessionCleanupWorker.KEEP_ALIVE_SEC,
                            SessionCleanupWorker.MAX_PERSISTENT_REQ));
                } else {
                    writer.appendHeader("Connection", "close");
                }

                try {
                    final ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
                    writer.appendHeader("Date", now.format(RFC_1123_DATE_TIME));
                } catch (DateTimeException dte) {
                    dte.printStackTrace();
                }

                responder.respond(req, writer);
            }

            if (keepAlive) {
                client.enableKeepAlive();
            } else {
                performClientDisconnected(chan);
            }
        } catch (Exception e) {
            performClientDisconnected(chan);
        }
    }
}

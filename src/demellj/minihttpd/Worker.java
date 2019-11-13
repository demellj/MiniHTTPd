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

    Worker(ConcurrentHashMap<SocketChannel, Client> sessions,
           AtomicBoolean isRunning,
           Selector selector,
           ThreadSafeResponder responder) {
        this.sessions = sessions;
        this.isRunning = isRunning;
        this.selector = selector;
        this.responder = responder;
    }

    @Override
    public void run() {
        while (isRunning.get()) {
            try {
                if (selector.select() > 0) {
                    final Set<SelectionKey> selectedKeys = selector.selectedKeys();

                    synchronized (selectedKeys) {
                        for (final SelectionKey key : selectedKeys) {
                            if (key.isValid()) {
                                if (key.isAcceptable() && key.channel() instanceof ServerSocketChannel)
                                    performClientAccept((ServerSocketChannel) key.channel());
                                else if (key.isReadable() && key.channel() instanceof SocketChannel)
                                    performClientRead((SocketChannel) key.channel());
                            }
                        }
                        selectedKeys.clear();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void performClientAccept(ServerSocketChannel serverChannel) {
        try {
            final SocketChannel clientChannel = serverChannel.accept();
            clientChannel.configureBlocking(false);
            clientChannel.register(selector, SelectionKey.OP_READ);
            sessions.put(clientChannel, new Client(clientChannel));
            System.err.println(String.format("%s connected", clientChannel.getRemoteAddress()));
        } catch (IOException e) {
            e.printStackTrace();
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

        if (client == null) return;

        final LineBuffer lineBuffer = client.getBuffers().getLineBuffer();

        try {
            lineBuffer.read(chan);

            Request req = null;
            while ((req = Request.Parser.parse(lineBuffer)) != null) {
                final ResponseWriter writer = new ResponseWriter("HTTP/1.1", client);

                final String connection = req.headers.get("connection");
                final boolean canKeepAlive = connection != null && connection.toLowerCase().contains("keep-alive");

                writer.appendHeader("Server", "MiniHTTPd");

                if (canKeepAlive) {
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

                try {
                    responder.respond(req, writer);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (canKeepAlive) {
                    client.enableKeepAlive();
                } else {
                    performClientDisconnected(chan);
                }
            }
        } catch (IOException e) {
            performClientDisconnected(chan);
        }
    }
}

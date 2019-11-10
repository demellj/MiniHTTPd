package demellj.minihttpd;

import java.io.IOException;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class Worker implements Runnable {
    private final ConcurrentHashMap<SocketChannel, Client> sessions;
    private final AtomicBoolean isRunning;
    private final Selector selector;
    private final ThreadSafeResponder responder;
    private final SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");

    public Worker(ConcurrentHashMap<SocketChannel, Client> sessions,
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
                selector.select((key) -> {
                    if (key.isValid()) {
                        if (key.isAcceptable() && key.channel() instanceof ServerSocketChannel) {
                            performClientAccept((ServerSocketChannel) key.channel());
                        } else if (key.isReadable() && key.channel() instanceof SocketChannel) {
                            performClientRead((SocketChannel) key.channel());
                        }
                    } else {
                        if (key.channel() instanceof SocketChannel) {
                            performClientDisconnected((SocketChannel) key.channel());
                        }
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void performClientAccept(ServerSocketChannel ch) {
        try {
            final var clientSocket = ch.accept();
            clientSocket.configureBlocking(false);
            clientSocket.register(selector, SelectionKey.OP_READ);
            sessions.put(clientSocket, new Client(clientSocket));
            System.err.println(String.format("%s connected", clientSocket.getRemoteAddress()));
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
        final var client = sessions.get(chan);

        if (client == null) return;

        final var lineBuffer = client.getLineBuffer();

        try {
            lineBuffer.read(chan);

            Request req = null;
            while ((req = Request.Parser.parse(lineBuffer)) != null) {
                var resp = responder.respond(req);
                if (resp == null) resp = Response.Factory.new404();

                resp.setProtocolVersion("HTTP/1.1");

                final var connection = req.headers.get("connection");
                final var canKeepAlive = connection != null && connection.toLowerCase().contains("keep-alive");

                resp.putHeader("Server", "MiniHTTPd");

                if (canKeepAlive) {
                    client.updateActivity();
                    resp.putHeader("Connection", "keep-alive");
                    resp.putHeader("Keep-Alive", String.format("timeout=%d, max=%d",
                            SessionCleanupWorker.KEEP_ALIVE_SEC,
                            SessionCleanupWorker.MAX_PERSISTENT_REQ));
                } else {
                    resp.putHeader("Connection", "close");
                }

                resp.putHeader("Date", format.format(Calendar.getInstance().getTime()).replace(".", ""));

                final byte[] content = resp.getBodyText().getBytes(StandardCharsets.UTF_8);

                resp.putHeader("Content-Length", Integer.toString(content.length));

                final byte[] header = resp.getHeaderText().getBytes(StandardCharsets.UTF_8);

                writeToClient(chan, header);
                writeToClient(chan, content);

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

    private void writeToClient(SocketChannel chan, byte[] data) throws IOException {
        final Client client = sessions.get(chan);

        if (client == null) return;

        final var out = client.getOutBuffer();

        var chunk_len = Math.min(data.length, out.capacity());

        int sent_bytes = 0;
        while (sent_bytes < data.length && chan.isOpen()) {
            out.clear();
            out.put(data, sent_bytes, chunk_len);
            out.flip();

            var sent = 0;
            do {
                sent += chan.write(out);
            } while (sent < chunk_len && chan.isOpen());

            sent_bytes += sent;

            chunk_len = Math.min(data.length - sent_bytes, chunk_len);
        }
        out.clear();
    }
}

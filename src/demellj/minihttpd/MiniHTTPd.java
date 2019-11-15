package demellj.minihttpd;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MiniHTTPd {
    private AtomicBoolean isRunning = new AtomicBoolean(false);

    private ArrayList<Thread> threadPool = new ArrayList<>();

    private Selector selector;
    private Selector acceptSelector;
    private ServerSocketChannel serverChannel;
    private WorkerSync sync = null;

    private final ConcurrentHashMap<SocketChannel, Client> sessions = new ConcurrentHashMap<>();

    private final ThreadSafeResponder safeResponder = new ThreadSafeResponder();

    private final Runnable server = new Runnable() {
        @Override
        public void run() {
            while (isRunning.get()) {
                try {
                    if (acceptSelector.select() > 0) {
                        final Set<SelectionKey> selectedKeys = acceptSelector.selectedKeys();
                        for (final SelectionKey key : selectedKeys) {
                            if (key.isValid() && key.isAcceptable())
                                performClientAccept((ServerSocketChannel) key.channel());
                            selectedKeys.remove(key);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void performClientAccept(ServerSocketChannel serverChannel) {
            try {
                final SocketChannel chan = serverChannel.accept();
                if (chan != null && sessions.putIfAbsent(chan, new Client(chan)) == null) {
                    chan.configureBlocking(false);
                    sync.activate();
                    selector.wakeup();
                    chan.register(selector, SelectionKey.OP_READ);
                    sync.signalAndWait();
                    System.err.println(String.format("%s connected", chan.getRemoteAddress()));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    /**
     * Creates a new http server bound to the specified port.
     *
     * @param port
     * @throws IOException
     */
    public MiniHTTPd(int port) throws IOException {
        try {
            selector = Selector.open();
            acceptSelector = Selector.open();
            serverChannel = ServerSocketChannel.open();
            serverChannel.bind(new InetSocketAddress(port));
            serverChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            serverChannel.configureBlocking(false);
            serverChannel.register(acceptSelector, SelectionKey.OP_ACCEPT);
        } catch (IOException ioe) {
            if (serverChannel != null) serverChannel.close();
            if (selector != null) selector.close();
            if (acceptSelector != null) acceptSelector.close();
            throw ioe;
        }
    }

    /**
     * Start serving. Employs workers to manage requests.
     *
     * @param numWorkers number of workers to employ
     */
    public void startup(int numWorkers) {
        if (numWorkers > 0) {
            if (isRunning.getAndSet(true))
                return;

            sync = new WorkerSync(numWorkers + 1);

            for (int i = 1; i <= numWorkers; ++i) {
                final Thread thread = new Thread(new Worker(sessions, isRunning, selector, safeResponder, sync));
                thread.setName("Worker " + i);
                threadPool.add(thread);
                thread.start();
            }

            final Thread cleanupWorker = new Thread(new SessionCleanupWorker(sessions, isRunning));
            threadPool.add(cleanupWorker);
            cleanupWorker.setName("Cleanup Worker");
            cleanupWorker.start();

            final Thread serverThread = new Thread(server);
            serverThread.setName("Server");
            serverThread.start();
        }
    }

    /**
     * Stop serving. Frees all workers.
     */
    public void shutdown() throws InterruptedException {
        isRunning.set(false);
        selector.wakeup();

        for (final Thread thread : threadPool) {
            thread.join();
        }

        threadPool.clear();
    }

    /**
     * Change the server's responder.
     *
     * @param resp Used by workers to manage requests.
     */
    public void setResponder(Responder resp) {
        safeResponder.setResponder(resp);
    }

    /**
     * Unbinds this server from the given port.
     *
     * @throws IOException
     */
    public void unbind() throws IOException {
        if (serverChannel != null && serverChannel.isOpen()) {
            serverChannel.close();
            serverChannel = null;
        }
        if (selector != null) {
            selector.close();
            selector = null;
        }
        if (acceptSelector != null) {
            acceptSelector.close();
            acceptSelector = null;
        }
    }
}

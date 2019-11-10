package demellj.minihttpd;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MiniHTTPd {
	private AtomicBoolean isRunning = new AtomicBoolean(false);
	
	private ReadWriteLock mRespRWLock = new ReentrantReadWriteLock();
	private ExecutorService pool = null;

	private Selector selector;
	private ServerSocketChannel serverChannel;

	private final ConcurrentHashMap<SocketChannel, Client> sessions = new ConcurrentHashMap<>();

	private final ThreadSafeResponder safeResponder = new ThreadSafeResponder();

	/**
	 * Creates a new http server bound to the specified port.
	 * 
	 * @param port
	 * @throws IOException
	 */
	public MiniHTTPd(int port) throws IOException {
		try {
			selector = Selector.open();
			serverChannel = ServerSocketChannel.open();
			serverChannel.bind(new InetSocketAddress(port));
			serverChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
			serverChannel.setOption(StandardSocketOptions.SO_REUSEPORT, true);
			serverChannel.configureBlocking(false);
			serverChannel.register(selector, SelectionKey.OP_ACCEPT);
		} catch (IOException ioe) {
			if (serverChannel != null) serverChannel.close();
			if (selector != null) selector.close();
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

			pool = Executors.newFixedThreadPool(numWorkers + 1);
			for (int i = 0; i < numWorkers; ++i)
				pool.submit(new Worker(sessions, isRunning, selector, safeResponder));

			pool.submit(new SessionCleanupWorker(sessions, isRunning));
		}
	}
	
	/**
	 * Stop serving. Frees all workers.
	 */
	public void shutdown() {
		isRunning.set(false);
		selector.wakeup();

		pool.shutdown();
		pool = null;
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
	}
}

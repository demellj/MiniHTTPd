package demellj.minihttpd;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MiniHTTPd {
	private int miPort = 9000;
	private int miNumWorkers = 8;
	
	private ServerSocket mSocket = null;
	private Thread serverThread = null;
	private boolean mRunServer = true;
	
	private ReadWriteLock mRespRWLock = new ReentrantReadWriteLock();
	private ReentrantLock mCtrlLock = new ReentrantLock();
	private ExecutorService pool = null;
	
	private Responder mResponder = new Responder() {
		@Override
		public Response respond(Request req) {
			return Response.Factory.new404();
		}
	};
	
	private Runnable server = new Runnable() {
		@Override
		public void run() {
			if (mSocket == null) return;
			pool = Executors.newFixedThreadPool(miNumWorkers);
			System.out.println("*** Server started");
			
			while (true) {
				// Check to quit server
				mCtrlLock.lock(); try {
					if (!mRunServer || pool == null) {
						System.out.println("*** Server shutdown");
						break;
					}
				} finally {
					mCtrlLock.unlock();
				}
				
				try {
					System.out.println("*** Waiting for client!");
					Socket client = mSocket.accept();
					Request req = Request.Parser.parse(client.getInputStream());
					if (req != null)
						pool.execute(new Worker(client, req));
					else
						client.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	};
	
	private class Worker implements Runnable {
		private final Socket mClient;
		private final Request mRequest;
		
		private Worker(Socket client, Request req) {
			mClient = client;
			mRequest = req;
		}
		
		@Override
		public void run() {
			System.out.println("*** Worker " + Thread.currentThread().getName() + " serving client " + mClient.getInetAddress().getHostName()  + "/" + mClient.getPort());
			
			Response resp = null;
			
			mRespRWLock.readLock().lock(); try {
				resp = mResponder.respond(mRequest);
			} finally {
				mRespRWLock.readLock().unlock();
			}
			
			try {
				PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(mClient.getOutputStream())));
				if (resp != null) {
					out.print(resp.getRawText());
				} else {
					out.print(Response.Factory.new404().getRawText());
				}
				out.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			try {
				mClient.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Creates a new http server bound to the specified port.
	 * 
	 * @param port
	 * @throws IOException
	 */
	public MiniHTTPd(int port) throws IOException {
		miPort = port;
		mSocket = new ServerSocket(miPort);
	}
	
	/**
	 * Start serving. Employs workers to manage requests.
	 * 
	 * @param numWorkers number of workers to employ
	 */
	public void startup(int numWorkers) {
		if (serverThread != null && serverThread.isAlive()) return;
		
		miNumWorkers = numWorkers;
		mRunServer = true;
		serverThread = new Thread(server);
		serverThread.start();
	}
	
	/**
	 * Stop serving. Frees all workers.
	 */
	public void shutdown() {
		mCtrlLock.lock(); try {
			mRunServer = false;
		} finally {
			mCtrlLock.unlock();
		}
		
		serverThread = null;
		pool.shutdown();
		pool = null;
	}
	
	/**
	 * Change the server's responder. 
	 * 
	 * @param resp Used by workers to manage requests.
	 */
	public void setResponder(Responder resp) {
		mRespRWLock.writeLock().lock(); try {
			if (resp == null) {
				mResponder = new Responder() {
					@Override
					public Response respond(Request req) {
						return Response.Factory.new404();
					}
				};
			} else {
				mResponder = resp;
			}
		} finally {
			mRespRWLock.writeLock().unlock();
		}
	}
	
	/**
	 * Unbinds this server from the given port.
	 * @throws IOException
	 */
	public void unbind() throws IOException {
		if (mSocket != null && mSocket.isBound()) {
			mSocket.close();
			mSocket = null;
		}
	}
}

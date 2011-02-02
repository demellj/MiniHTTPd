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
	private ServerSocket mSocket = null;
	private Thread serverThread = null;
	private boolean mRunServer = true;
	
	private ReadWriteLock mRespRWLock = new ReentrantReadWriteLock();
	private ReentrantLock mCtrlLock = new ReentrantLock();
	private ExecutorService pool = null;
	
	private Responder mResponder = null;
	
	private Runnable server = new Runnable() {
		@Override
		public void run() {
			if (mSocket == null) return;
			pool = Executors.newFixedThreadPool(16);
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
					if (mResponder == null) {
						client.close();
						continue;
					}
					Request req = Request.Parser.parse(client.getInputStream());
					pool.execute(new Worker(client, req));
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
			
			if (resp != null) {
				PrintWriter out;
				try {
					out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(mClient.getOutputStream())));
					out.print(resp.getRawText());
					out.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			try {
				mClient.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public MiniHTTPd(int port) throws IOException {
		miPort = port;
		mSocket = new ServerSocket(miPort);
	}
	
	public void startup() {
		if (serverThread != null && serverThread.isAlive()) return;
		
		mRunServer = true;
		serverThread = new Thread(server);
		serverThread.start();
	}
	
	public void shutdown() {
		mCtrlLock.lock(); try {
			mRunServer = false;
		} finally {
			mCtrlLock.unlock();
		}
		
		mResponder = null;
		serverThread = null;
		pool.shutdown();
	}
	
	public void setResponder(Responder resp) {
		mRespRWLock.writeLock().lock(); try {
			mResponder = resp;
		} finally {
			mRespRWLock.writeLock().unlock();
		}
	}
}

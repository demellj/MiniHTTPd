package demellj.minihttpd;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ThreadSafeResponder implements Responder {
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private Responder responder = null;

    public void setResponder(Responder resp) {
        lock.writeLock().lock();
        try {
            responder = resp;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Response respond(Request req) {
        lock.readLock().lock();
        try {
            final Responder responder = this.responder;
            if (responder != null)
                return responder.respond(req);
            else
                return Response.Factory.new404();
        } finally {
            lock.readLock().unlock();
        }
    }
}

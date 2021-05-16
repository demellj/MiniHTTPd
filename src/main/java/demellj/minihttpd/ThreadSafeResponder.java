package demellj.minihttpd;

import demellj.minihttpd.response.IllegalBuilderStateException;

import java.io.IOException;
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
    public void respond(Request req, ResponseWriter writer) {
        lock.readLock().lock();
        try {
            final Responder responder = this.responder;
            if (responder != null) {
                responder.respond(req, writer);
            } else {
                try {
                    writer.begin(HTTPStatus.HTTP_404).emptyBody();
                } catch (IOException | IllegalBuilderStateException e) {
                    e.printStackTrace();
                }
            }
        } finally {
            lock.readLock().unlock();
        }
    }
}

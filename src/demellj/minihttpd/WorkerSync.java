package demellj.minihttpd;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicBoolean;

public class WorkerSync {
    final private CyclicBarrier barrier;
    final private AtomicBoolean isActive = new AtomicBoolean(false);

    public WorkerSync(int numWorkers) {
        this.barrier = new CyclicBarrier(numWorkers, () -> {
            isActive.set(false);
        });
    }

    public boolean signalAndWait() {
        if (isActive.get()) {
            try {
                barrier.await();
                return true;
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (BrokenBarrierException e) {
                e.printStackTrace();
            }
            return false;
        } else {
            return true;
        }
    }

    public void activate() {
        isActive.set(true);
    }
}

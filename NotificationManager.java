import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class NotificationManager implements Runnable {

    private Point point;
    private List<Point> destinos;

    private DataOutputStream out;

    private ReentrantLock rewards_lock;
    private Condition rewards_condition;

    private boolean waiting;


    public NotificationManager(DataOutputStream out, ServerManager manager, Point p) {
        this.point = p;
        destinos = new ArrayList<>();
        this.out = out;
        rewards_lock = manager.getRewardsMapLock();
        rewards_condition = rewards_lock.newCondition();
        waiting = true;
    }

    public void signalNotification() {
        rewards_lock.lock();
        try {
            waiting = false;
            rewards_condition.signal();
        } finally {
            rewards_lock.unlock();
        }
    }

    public void run() {

        while (true) {
            try {
                rewards_lock.lock();
                try {
                    while (waiting)
                        rewards_condition.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException("Thread Interrupted");
                }


                List<Reward> rewards = new ArrayList<>();
                for (Reward r : ServerManager.listRewardsOnCoords(point))
                    if (!(destinos.contains(r.getDestino()))) {
                        destinos.add(r.getDestino().clone());
                        rewards.add(r);
                    }

                if (rewards.size() > 0) {
                    out.writeUTF("Encontradas Recompensas para si: " + rewards);
                    out.flush();
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            finally {
                waiting = true;
                rewards_lock.unlock();
            }
        }

    }




}












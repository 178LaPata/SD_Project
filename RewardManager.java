import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class RewardManager implements Runnable {

    private static ReentrantLock rewards_lock;

    private static Condition rewards_condition;

    private static boolean waiting;

    public RewardManager(ServerManager manager) {
        rewards_lock = manager.getRewardsMapLock();
        rewards_condition = rewards_lock.newCondition();
        waiting = true;
    }

    public static void signalRewardManager() {
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
                    throw new RuntimeException(e);
                }
                ServerManager.updateRewards();
            } finally {
                waiting = true;
                rewards_lock.unlock();
            }
        }
    }



}














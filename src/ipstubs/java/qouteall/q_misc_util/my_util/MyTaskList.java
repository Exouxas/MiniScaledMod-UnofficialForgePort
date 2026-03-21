package qouteall.q_misc_util.my_util;

public class MyTaskList {

    /**
     * Mirror of the real MyTaskList.MyTask inner interface in ImmPTL.
     * The real addTask(MyTask) uses this type — NOT BooleanSupplier.
     */
    public interface MyTask {
        boolean runAndGetIsFinished();

        default void onCancelled() {}
    }

    public void addTask(MyTask task) {
        // stub — no-op
    }

    public void addOneShotTask(Runnable runnable) {
        // stub — no-op
    }

    public static MyTask oneShotTask(Runnable runnable) {
        return () -> {
            if (runnable != null) {
                runnable.run();
            }
            return true;
        };
    }
}

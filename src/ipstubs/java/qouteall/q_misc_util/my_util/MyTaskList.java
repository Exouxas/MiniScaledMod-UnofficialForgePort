package qouteall.q_misc_util.my_util;

import java.util.function.BooleanSupplier;

public class MyTaskList {
    public void addTask(BooleanSupplier task) {
    }

    public static BooleanSupplier oneShotTask(Runnable runnable) {
        return () -> {
            if (runnable != null) {
                runnable.run();
            }
            return true;
        };
    }
}

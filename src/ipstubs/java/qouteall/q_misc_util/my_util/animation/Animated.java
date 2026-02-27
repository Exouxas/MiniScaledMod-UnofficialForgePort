package qouteall.q_misc_util.my_util.animation;

import java.util.function.DoubleUnaryOperator;
import java.util.function.LongSupplier;

public class Animated<T> {
    public static final TypeInfo<Double> DOUBLE_TYPE_INFO = new TypeInfo<>();

    public static class TypeInfo<T> {
    }

    private T current;
    private T target;

    public Animated(TypeInfo<T> typeInfo, LongSupplier timeSupplier, DoubleUnaryOperator progressMapper, T initialValue) {
        this.current = initialValue;
        this.target = initialValue;
    }

    public T getCurrent() {
        return current;
    }

    public T getTarget() {
        return target;
    }

    public void setTarget(T target, long durationNano) {
        this.target = target;
        this.current = target;
    }
}

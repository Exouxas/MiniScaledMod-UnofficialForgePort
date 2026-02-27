package qouteall.q_misc_util.my_util.animation;

public class Animated<T> {
    public static final TypeInfo<Double> DOUBLE_TYPE_INFO = new TypeInfo<>();

    public static class TypeInfo<T> {
    }

    /** Functional interface matching qouteall.q_misc_util.my_util.animation.Animated$TimeSupplier */
    public interface TimeSupplier {
        long getTime();
    }

    /** Functional interface matching qouteall.q_misc_util.my_util.animation.Animated$TimingFunction */
    public interface TimingFunction {
        double apply(double progress);
    }

    private T current;
    private T target;

    public Animated(TypeInfo<T> typeInfo, TimeSupplier timeSupplier, TimingFunction timingFunction, T initialValue) {
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

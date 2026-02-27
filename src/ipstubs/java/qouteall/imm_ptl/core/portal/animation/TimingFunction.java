package qouteall.imm_ptl.core.portal.animation;

public class TimingFunction {
    public interface Mapper {
        double mapProgress(double progress);
    }

    public static final Mapper sine = progress -> progress;
}

package qouteall.imm_ptl.core.portal.animation;

public enum TimingFunction {
    linear,
    sine,
    circle,
    easeInOutCubic;

    public double mapProgress(double progress) {
        return progress;
    }
}

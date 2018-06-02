package recorder.record.triggers;


import recorder.common.MediaType;
import recorder.record.Trigger;


public abstract class MotionDetector extends Trigger {

    public static final double DEFAULT_THRESHOLD_MIN = 0.2;
    public static final double DEFAULT_THRESHOLD_MAX = 40;

    /**
     * Percent of frame change [0,100].
     */
    private final double thresholdMin;
    private final double thresholdMax;

    public MotionDetector(long durationBefore, long durationAfter, double thresholdMin, double thresholdMax) {
        super(durationBefore, durationAfter);
        if (thresholdMin < 0) {
            throw new IllegalArgumentException("Threshold min value less than 0%.");
        } else if (thresholdMax > 100) {
            throw new IllegalArgumentException("Threshold max value greater than 100%.");
        } else if (thresholdMin >= thresholdMax) {
            throw new IllegalArgumentException("Threshold min value greater or equal max value.");
        }
        this.thresholdMin = thresholdMin;
        this.thresholdMax = thresholdMax;
    }

    @Override
    public final MediaType getMediaType() {
        return MediaType.VIDEO;
    }

    public double getThresholdMin() {
        return thresholdMin;
    }

    public double getThresholdMax() {
        return thresholdMax;
    }

}

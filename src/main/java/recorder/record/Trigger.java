package recorder.record;

import recorder.common.HasMediaType;
import recorder.common.UncompatibleMediaTypeException;
import recorder.sampler.Sample;

public abstract class Trigger implements HasMediaType {

    /**
     *  Milliseconds.
     */
    public static final long DEFAULT_DURATION_AFTER = 20000;
    public static final long DEFAULT_DURATION_BEFORE = 10000;
    private final long durationBefore;
    private final long durationAfter;

    public Trigger(long durationBefore, long durationAfter) {
        if (durationBefore < 0 || durationAfter < 0) {
            throw new IllegalArgumentException();
        }
        this.durationBefore = durationBefore;
        this.durationAfter = durationAfter;
    }

    public long getDurationAfter() {
        return durationAfter;
    }

    public long getDurationBefore() {
        return durationBefore;
    }

    public final boolean check(Sample sample) {
        if (sample == null) {
            throw new IllegalArgumentException();
        }
        if (!sample.getMediaType().isCompatible(this.getMediaType())) {
            throw new UncompatibleMediaTypeException(
                    "Unsupported sample madia type.",
                    sample.getMediaType(),
                    this.getMediaType()
            );
        }
        return _check(sample);
    }

    protected abstract boolean _check(Sample sample);

}

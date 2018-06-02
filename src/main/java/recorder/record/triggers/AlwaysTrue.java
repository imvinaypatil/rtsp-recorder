package recorder.record.triggers;

import recorder.common.MediaType;
import recorder.record.Trigger;
import recorder.sampler.Sample;

public class AlwaysTrue extends Trigger {

    public AlwaysTrue(long durationBefore, long durationAfter) {
        super(durationBefore, durationAfter);
    }

    @Override
    public MediaType getMediaType() {
        return MediaType.VIDEO_AND_AUDIO;
    }

    @Override
    public boolean _check(Sample sample) {
        return true;
    }

}

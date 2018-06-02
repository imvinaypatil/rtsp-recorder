package recorder.record;

import recorder.sampler.Sample;
import recorder.sampler.SampleHandler;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;


public class Recorder implements SampleHandler {

    private final ArrayList<Trigger> triggers = new ArrayList<>();
    private RecordHandler recordHandler;
    private Date triggeredRecordBegin = null;
    private Date triggeredRecordEnd = null;
    private boolean record = false;
    private final HashSet<Sample> skippedSamples = new HashSet<>();
    private boolean enabled = true;

    public RecordHandler getRecordHandler() {
        return recordHandler;
    }

    public void setRecordHandler(RecordHandler recordHandler) {
        this.recordHandler = recordHandler;
    }

    public ArrayList<Trigger> getTriggers() {
        return triggers;
    }

    @Override
    public void onSample(Sample sample) {
        if (!enabled) {
            if (record) {
                if (recordHandler != null) {
                    recordHandler.onRecordStop();
                }
                record = false;
            }
            return;
        }

        // In any case we should check triggers because of possible
        // triggeredRecordBegin/triggeredRecordEnd change.
        // Even if sample intersects with area of recorder.record, this sample could
        // change it for future samples.
        checkTriggers(sample);

        if (triggeredRecordEnd == null || sample.getBegin().after(triggeredRecordEnd)) {
            // If there was no triggering or trigger action time ended.
            // Finalize recorder.record if it is not finalized yet.
            if (record) {
                if (recordHandler != null) {
                    recordHandler.onRecordStop();
                }
                record = false;
            }
            skippedSamples.add(sample);
        } else {
            // Record sample.
            record = true;

            // triggeredRecordBegin changed after triggering in checkTriggers().
            // We should check skippedSamples. May be some of them should
            // be added in recorder.record.
            skippedSamples.stream()
                    .filter(
                            skippedSample -> !skippedSample.getEnd().before(triggeredRecordBegin)
                    )
                    .forEach(skippedSample -> {
                        if (recordHandler != null) {
                            recordHandler.onRecord(skippedSample);
                        }
                    });

            if (recordHandler != null) {
                recordHandler.onRecord(sample);
            }
        }

        clearOldSkippedSamples(sample.getEnd());
    }

    private void clearOldSkippedSamples(Date startingPoint) {
        if (skippedSamples.isEmpty()) {
            return;
        }

        long maxDurationBefore = 0;
        for (Trigger trigger : triggers) {
            maxDurationBefore = Math.max(trigger.getDurationBefore(), maxDurationBefore);
        }

        Date minTriggeredRecordBegin = new Date(startingPoint.getTime() - maxDurationBefore);

        Iterator<Sample> skippedSamplesIterator = skippedSamples.iterator();
        while (skippedSamplesIterator.hasNext()) {
            Sample skippedSample = skippedSamplesIterator.next();
            if (skippedSample.getEnd().before(minTriggeredRecordBegin)) {
                skippedSamplesIterator.remove();
            }
        }
    }

    private void checkTriggers(Sample sample) {
        long maxDurationBefore = -1;
        long maxDurationAfter = -1;
        for (Trigger trigger : triggers) {
            if (checkTrigger(sample, trigger)) {
                maxDurationBefore = Math.max(trigger.getDurationBefore(), maxDurationBefore);
                maxDurationAfter = Math.max(trigger.getDurationAfter(), maxDurationAfter);
            }
        }
        if (maxDurationBefore != -1 && maxDurationAfter != -1) {
            triggeredRecordBegin = new Date(sample.getBegin().getTime() - maxDurationBefore);
            triggeredRecordEnd = new Date(sample.getBegin().getTime() + maxDurationAfter);
        }
    }

    private boolean checkTrigger(Sample sample, Trigger trigger) {
        if (!trigger.getMediaType().isCompatible(sample.getMediaType())) {
            return false;
        }
        return trigger.check(sample);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

}

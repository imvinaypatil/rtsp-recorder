package recorder.record;


import recorder.sampler.Sample;

public interface RecordHandler {

    public void onRecord(Sample sample);

    public default void onRecordStop() {
    }

}

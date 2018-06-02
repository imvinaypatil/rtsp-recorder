package recorder;public interface IRecordListener {
    public void onRecord(RecordInvoker.TYPE type);
    public void onStop(RecordInvoker.TYPE type);
    public void onCrash(RecordInvoker.TYPE type);
}

package recorder;import recorder.common.Executor;

import java.io.File;

public interface ISamplerListener {
    public void onStart(Executor executor, RecordInvoker.TYPE type);
    default public void onStop(Executor executor, RecordInvoker.TYPE type){ };
    public void onStoping(Executor executor, RecordInvoker.TYPE type);
    public void onCrash(Executor executor, RecordInvoker.TYPE type);
    default void onStop(Executor executor, RecordInvoker.TYPE type, File file){
        onStop(executor,type);
    };
}

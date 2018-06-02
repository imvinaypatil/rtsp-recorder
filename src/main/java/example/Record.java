package example;

import recorder.Device;
import recorder.RecordInvoker;

public class Record {

    public static void main(String args[] ) throws Exception {
        Device device = new Device("TEST","rtsp://184.72.239.149/vod/mp4:BigBuckBunny_115k.mov",null,"tcp",System.getProperty("user.dir")+"\\video");
        device.triggerRecording(true,RecordInvoker.TYPE.ALWAYS);
        Thread.sleep(1000*60);
        device.triggerRecording(false,RecordInvoker.TYPE.ALWAYS);
    }
}

package recorder.engine.stream_chunker.simplified_grabber;

import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacpp.avformat;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;

public interface SimplifiedGrabber {

    public Frame grab() throws FrameGrabber.Exception;

    public avcodec.AVPacket grabPacket() throws FrameGrabber.Exception;

    public avformat.AVFormatContext getFormatContext();

    public int getAudioChannels();

    public int getImageWidth();

    public int getImageHeight();

    public int getFps();

    public void start() throws FrameGrabber.Exception;

    public void stop() throws FrameGrabber.Exception;
}

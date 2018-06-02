package recorder.engine.stream_chunker.simplified_grabber;

import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacpp.avformat;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;

import java.util.Objects;

public class SimplifiedGrabberWrap implements SimplifiedGrabber {

    private final FFmpegFrameGrabber grabber;

    public SimplifiedGrabberWrap(FFmpegFrameGrabber grabber) {
        Objects.requireNonNull(grabber);
        this.grabber = grabber;
    }

    @Override
    public Frame grab() throws FrameGrabber.Exception {
        Frame frame = grabber.grab();
        if (frame == null) {
            return null;
        }
        frame.timestamp = grabber.getTimestamp();
        return frame;
    }

    @Override
    public avcodec.AVPacket grabPacket() throws FrameGrabber.Exception {
        avcodec.AVPacket packet = grabber.grabPacket();
        if (packet == null) {
            return null;
        }
//        frame.timestamp = grabber.getTimestamp();
        return packet;
    }

    @Override
    public avformat.AVFormatContext getFormatContext() {
        return this.grabber.getFormatContext();
    }

    @Override
    public int getAudioChannels() {
        return grabber.getAudioChannels();
    }

    @Override
    public int getImageWidth() {
        return grabber.getImageWidth();
    }

    @Override
    public int getImageHeight() {
        return grabber.getImageHeight();
    }

    @Override
    public int getFps() {
        return (int) grabber.getFrameRate();
    }

    @Override
    public void start() throws FrameGrabber.Exception {
        grabber.start();
    }

    @Override
    public void stop() throws FrameGrabber.Exception {
        grabber.stop();
        grabber.release();
    }

    protected FrameGrabber getGrabber() {
        return grabber;
    }

}

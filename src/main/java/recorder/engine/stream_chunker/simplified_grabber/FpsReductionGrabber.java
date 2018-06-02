package recorder.engine.stream_chunker.simplified_grabber;

import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacpp.avformat;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.OpenCVFrameConverter;

import java.util.Objects;


public class FpsReductionGrabber implements SimplifiedGrabber {

    private final SimplifiedGrabber simplifiedGrabber;
    private long timestamp = 0;
    private final int fps;
    private final OpenCVFrameConverter.ToMat matConverter = new OpenCVFrameConverter.ToMat();
    private opencv_core.Mat mat = null;

    public FpsReductionGrabber(SimplifiedGrabber simplifiedGrabber, int fps) {
        Objects.requireNonNull(simplifiedGrabber);
        if (fps < 1) {
            throw new IllegalArgumentException();
        }
        this.simplifiedGrabber = simplifiedGrabber;
        this.fps = fps;
    }

    @Override
    public int getAudioChannels() {
        return simplifiedGrabber.getAudioChannels();
    }

    @Override
    public int getImageWidth() {
        return simplifiedGrabber.getImageWidth();
    }

    @Override
    public int getImageHeight() {
        return simplifiedGrabber.getImageHeight();
    }

    @Override
    public void start() throws FrameGrabber.Exception {
        simplifiedGrabber.start();
    }

    @Override
    public void stop() throws FrameGrabber.Exception {
        simplifiedGrabber.stop();
    }

    @Override
    public Frame grab() throws FrameGrabber.Exception {
        Frame grabbedFrame;
        Frame outFrame;
        while (true) {
            grabbedFrame = simplifiedGrabber.grab();
            if (grabbedFrame == null) {
                return null;
            }
            if (grabbedFrame.image != null) {
                if (grabbedFrame.timestamp >= timestamp) {
                    if (mat != null) {
                        mat.release();
                    }
                    mat = matConverter.convert(grabbedFrame);
                    outFrame = matConverter.convert(mat);
                    outFrame.timestamp = timestamp;
                    outFrame.keyFrame = true;
                    timestamp += Math.round(1000000 / fps);
                } else {
                    continue;
                }
            } else {
                outFrame = grabbedFrame;
            }
            return outFrame;
        }

    }

    @Override
    public avcodec.AVPacket grabPacket() throws FrameGrabber.Exception {
        avcodec.AVPacket grabbedPacket;
        while (true) {
            grabbedPacket = simplifiedGrabber.grabPacket();
            if (grabbedPacket == null) {
                return null;
            }
            return grabbedPacket;
        }
    }

    @Override
    public avformat.AVFormatContext getFormatContext() {
        return this.simplifiedGrabber.getFormatContext();
    }

    @Override
    public int getFps() {
        return fps;
    }

}

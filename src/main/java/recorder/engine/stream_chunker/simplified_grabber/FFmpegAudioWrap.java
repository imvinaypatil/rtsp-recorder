package recorder.engine.stream_chunker.simplified_grabber;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;


public class FFmpegAudioWrap extends SimplifiedGrabberWrap {

    public FFmpegAudioWrap(FFmpegFrameGrabber grabber) {
        super(grabber);
    }

    @Override
    public Frame grab() throws FrameGrabber.Exception {
        Frame frame = ((FFmpegFrameGrabber) getGrabber()).grabSamples();
        frame.timestamp = getGrabber().getTimestamp();
        return frame;
    }

    
}

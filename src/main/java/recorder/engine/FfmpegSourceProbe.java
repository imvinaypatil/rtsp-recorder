package recorder.engine;


import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FrameGrabber;
import recorder.channel.Source;
import recorder.channel.SourceProbe;
import recorder.channel.sources.Rtsp;
import recorder.common.MediaType;

import java.util.logging.Level;
import java.util.logging.Logger;


public class FfmpegSourceProbe extends SourceProbe {

    private static final Logger LOG = Logger.getLogger(FfmpegSourceProbe.class.getName());

    /**
     * In microseconds.
     */
    private final static int TIMEOUT = 3000000;

    public FfmpegSourceProbe(Source source) {
        super(source);
    }

    @Override
    protected void initSourceInfo() {
        FFmpegFrameGrabber grabber = null;
        try {
            if (!(getSource() instanceof Rtsp)) {
                throw new RuntimeException(
                        String.format(
                                "Unsupproted class \"%s\".",
                                getSource().getClass().getName()
                        )
                );
            }
            Rtsp rtspSource = (Rtsp) getSource();
            grabber = new FFmpegFrameGrabber(rtspSource.getUri());
            grabber.setOption("stimeout", String.valueOf(TIMEOUT));
            grabber.start();
            boolean video = grabber.getVideoCodec() != 0;
            boolean audio = grabber.getAudioCodec() != 0;
            if (video && audio) {
                mediaType = MediaType.VIDEO_AND_AUDIO;
            } else {
                if (video) {
                    mediaType = MediaType.VIDEO;
                } else if (audio) {
                    mediaType = MediaType.AUDIO;
                } else {
                    throw new RuntimeException("Ffmpeg probe has not determined media tipe.");
                }
            }
        } catch (RuntimeException | FrameGrabber.Exception e) {
            throw new RuntimeException("Error during probbing.", e);
        } finally {
            try {
                if (grabber != null) {
                    grabber.stop();
                    grabber.release();
                }
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Error in FfmpegSourceProbe on grabber stop/release.", e);
            }
        }
    }

}

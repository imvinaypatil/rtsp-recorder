package recorder.engine;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import recorder.Device;
import recorder.channel.Channel;
import recorder.channel.Source;
import recorder.channel.sources.Rtsp;
import recorder.common.FileNameFunstions;
import recorder.engine.stream_chunker.StreamingChunker;
import recorder.engine.stream_chunker.simplified_grabber.*;
import recorder.sampler.SamplerEngine;

import java.io.File;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FfmpegSamplerEngine extends SamplerEngine {

    private static final Logger LOG = Logger.getLogger(FfmpegSamplerEngine.class.getName());

    public static int FPS = 15;

    /**
     * Sample duration in microseconds.
     */
    private final static int SAMPLE_SIZE = 30000000 * 2; // 30 sec * 2 roundOf to 60sec
    /**
     * In microseconds.
     */
    private final static int TIMEOUT = 3000000;
    private final StreamingChunker chunker;
    private ExecutorService executorService = null;

    Logger logger = Logger.getLogger(FfmpegSamplerEngine.class.getName());

    public FfmpegSamplerEngine(Channel channel, File tempDir) {
        super(channel, tempDir);
        boolean sameSource = false;
        if (channel.getVideoSource() != null && channel.getAudioSource() != null
                && getFfmpegUri(channel.getAudioSource()).equals(getFfmpegUri(channel.getVideoSource()))) {
            sameSource = true;
        }

        Supplier<SimplifiedGrabber> supplier;

        if (sameSource) {
            supplier = () -> {
                return new SimplifiedGrabberWrap(getFfmpegGrabber(getFfmpegUri(channel.getVideoSource()),channel.getTransport()));
            };
        } else {
            if (channel.getVideoSource() != null && channel.getAudioSource() != null) {
                supplier = () -> {
                    return new FFmpegMixerWrap(
                            getFfmpegGrabber(getFfmpegUri(channel.getVideoSource()), channel.getTransport()),
                            getFfmpegGrabber(getFfmpegUri(channel.getAudioSource()), null)
                    );
                };
            } else {
                if (channel.getVideoSource() != null) {
                    supplier = () -> {
                        return new FFmpegVideoWrap(getFfmpegGrabber(getFfmpegUri(channel.getVideoSource()),channel.getTransport()));
                    };
                } else {
                    supplier = () -> {
                        return new FFmpegAudioWrap(getFfmpegGrabber(getFfmpegUri(channel.getVideoSource()),channel.getTransport()));
                    };
                }
            }
        }

        chunker = new StreamingChunker(supplier, tempDir,SAMPLE_SIZE);
        chunker.setChunkHandler((chunkFile) -> {
            executorService.submit(() -> {
                getRawSampleHandler().accept(chunkFile, new Date(Long.valueOf(FileNameFunstions.withoutExtension(chunkFile.getName())) / 1000));
            });
        });

    }

    @Override
    public void run() {
        try {
            LOG.log(Level.FINER, "Attempt to start recorder.sampler recorder.engine \"{0}\".", getInfo());
            executorService = Executors.newSingleThreadExecutor();
            chunker.start();
            while (!isStoping()) {
                if (!chunker.next()) {
                    throw new RuntimeException("Chunker returned NULL.");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Engine is stopped on error.", e);
        } finally {
            try {
                chunker.stop();
            } catch (Exception exception) {
                LOG.log(Level.WARNING, "Can't stop chunker.", exception);
            }
            executorService.shutdown();
            executorService = null;
            LOG.log(Level.FINER, "Engine is stopped.");
        }
    }

    @Override
    public void runPacket() {
        try {
            LOG.log(Level.FINER, "Attempt to start recorder.sampler recorder.engine \"{0}\".", getInfo());
            executorService = Executors.newSingleThreadExecutor();
            chunker.startPacketRecord();
            while (!isStoping()) {
                if (!chunker.nextPacket()) {
                    throw new RuntimeException("Chunker returned NULL.");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Engine is stopped on error.", e);
        } finally {
            try {
                chunker.stop();
            } catch (Exception exception) {
                LOG.log(Level.WARNING, "Can't stop chunker.", exception);
            }
            executorService.shutdown();
            executorService = null;
            LOG.log(Level.FINER, "Engine is stopped.");
        }
    }

    private String getFfmpegUri(Source source) {
        if (source instanceof Rtsp) {
            return ((Rtsp) source).getUri();
        }
        throw new RuntimeException("Unsupported source device.");
    }

    private FFmpegFrameGrabber getFfmpegGrabber(String source, Device.Transport transport) {
        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(source);
        grabber.setOption("stimeout", String.valueOf(TIMEOUT));
        String rtsp_transport;
        switch (transport) {
            case UDP: rtsp_transport = "udp";
            break;
            case TCP: rtsp_transport = "tcp";
            break;
            default: rtsp_transport = "udp";
            break;
        }
        grabber.setOption("rtsp_transport", rtsp_transport);
        grabber.setOption("fflags", "genpts");
        grabber.setOption("fflags", "+genpts");
        LOG.log(Level.INFO, ("++Grabber Configuration++ \n" +
                "\t TIMEOUT " + grabber.getTimeout() + "\n" +
                "\t TRANSPORT " + ((transport != null) ? transport : ("UDP")) + "\n" +
//                "\t PIXEL_FORMAT " + grabber.getPixelFormat() + "\n" +
//                "\t FRAME_RATE " + grabber.getFrameRate() + "\n" +
//                "\t FORMAT " + grabber.getFormat() + "\n" +
//                "\t VCODEC " + grabber.getVideoCodec() + "\n" +
                "\t AUDIO_CHANNEL " + grabber.getAudioChannels()));
        return grabber;
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public String getName() {
        return "javacv-ffmpeg";
    }

}

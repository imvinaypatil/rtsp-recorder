package recorder.engine.stream_chunker;

import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import recorder.engine.stream_chunker.simplified_grabber.SimplifiedGrabber;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.bytedeco.javacpp.avutil.AV_PIX_FMT_YUV420P;

public class StreamingChunker {

    private Logger LOG = Logger.getLogger(StreamingChunker.class.getName());

    private final static long MIN_CHUNK_DURATION = 20000000;

    private Consumer<File> chunkHandler = null;
    private final FpsCalculator fpsCalculator = new FpsCalculator();
    private final ChunkDetector chunkDetector;
    private boolean hasVideo = false;
    private double fps = 20;
    private final Supplier<SimplifiedGrabber> grabberSupplier;
    private SimplifiedGrabber grabber = null;
    private FFmpegFrameRecorder recorder = null;
    private final List<Frame> probeFrames = new ArrayList<>();
    private final List<avcodec.AVPacket> probePackets = new ArrayList<>();
    private boolean started = false;
    private long begin = -1;

    public StreamingChunker(Supplier<SimplifiedGrabber> grabberSupplier, File targetDir) {
        this(grabberSupplier, targetDir, MIN_CHUNK_DURATION);
    }

    public StreamingChunker(Supplier<SimplifiedGrabber> grabberSupplier, File targetDir, long duration) {
        Objects.requireNonNull(grabberSupplier);
        Objects.requireNonNull(targetDir);
        if (duration < MIN_CHUNK_DURATION) {
            System.out.println(duration +"<" +"MIN_CHUNK_DURATION");
            throw new IllegalArgumentException();
        }
        targetDir.mkdirs();
        if (!targetDir.canWrite()) {
            throw new RuntimeException("Target directory isn't writable.");
        }
        this.grabberSupplier = grabberSupplier;
        chunkDetector = new ChunkDetector(duration);
        chunkDetector.setChunkListener(new ChunkListener() {

            File outputFile;

            @Override
            public void onChunkBegin() throws Exception {
//                if (hasVideo) {
//                    fps = Math.round(fpsCalculator.getFps() * 100) / 100d;
//                    fpsCalculator.reset();
//                }

                /*
                 *  Workaround for a bug present in javacv::FFmpegFrameGrabber.getFormatContext
                 *  This will inject the right fps into the recorder.
                 *  This may become redundant in future javacv142+ version
                 */
                grabber.getFormatContext().streams(0).r_frame_rate().num(grabber.getFps());

                outputFile = new File(targetDir, ( chunkDetector.getChunkBegin()) + ".avi");

                recorder = new FFmpegFrameRecorder(
                        outputFile,
                        grabber.getImageWidth(),
                        grabber.getImageHeight());
                recorder.setFormat("avi");
                recorder.setAudioChannels(grabber.getAudioChannels() > 0 ? 1 : 0);
//                recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
                recorder.setPixelFormat(AV_PIX_FMT_YUV420P);
                recorder.setVideoOption("threads", Integer.toString(Runtime.getRuntime().availableProcessors()));
                recorder.setVideoOption("tune", "zerolatency");
                recorder.setVideoOption("preset", "ultrafast");
                recorder.setVideoOption("crf", "22");
//                recorder.setGopSize((int)Math.round(fps * 5));
//                recorder.setVideoBitrate(56000);
//                recorder.setSampleRate(8000);
//                recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
//                recorder.setFrameRate(fps);
                recorder.start(grabber.getFormatContext());
            }

            @Override
            public void onChunkEnd() throws Exception {
                recorder.stop();
                recorder.release();
                if (chunkHandler != null) {
                    chunkHandler.accept(outputFile);
                }
                outputFile = null;
            }
        });
    }

    public void start() throws Exception {
        for (int pfn = 0, vfn = 0; pfn < 100 && vfn < 10; pfn++) {
            Frame frame = grab().clone();
            if (frame.image != null) {
                hasVideo = true;
                vfn++;
            }
            probeFrames.add(frame);
        }
        started = true;
    }

    public void stop() throws Exception {
        LOG.log(Level.INFO,"STOPPED");
        started = false;
        fpsCalculator.reset();
        chunkDetector.reset();
        hasVideo = false;
        fps = 20;
        begin = -1;
        if (grabber != null) {
            grabber.stop();
            grabber = null;
        }
        if (recorder != null) {
            recorder.stop();
            recorder = null;
            if (!probeFrames.isEmpty() || !probePackets.isEmpty()) {
                if (!probeFrames.isEmpty()) {
                    probeFrames.clear();
                }
                if (!probePackets.isEmpty()) {
                    probePackets.clear();
                }
                System.gc();
            }
        }
    }

    public boolean next() throws Exception {
        if (!started) {
            throw new IllegalStateException();
        }
        Frame frame;
        if (!probeFrames.isEmpty()) {
            frame = probeFrames.remove(0);
            if (probeFrames.isEmpty()) {
                System.gc();
            }
        } else {
            frame = grab();
        }
        if (frame == null) {
            return false;
        }
        record(frame);
        return true;
    }

    private Frame grab() throws Exception {
        if (grabber == null) {
            grabber = grabberSupplier.get();
            grabber.start();

            begin = System.currentTimeMillis() * 1000;
        }

        Frame frame = grabber.grab();
        if (frame != null) {
            if (frame.image != null) {
                fpsCalculator.addTimestamp(frame.timestamp);
            }
        }

        return frame;
    }

    private void record(Frame frame) throws Exception {
        chunkDetector.next(frame.keyFrame && frame.image != null, frame.timestamp);
        recorder.record(frame);
    }

    /*
     *  Recording Only Packet - Without transcoding
     */

    public void startPacketRecord() throws Exception {
        for (int pfn = 0, vfn = 0; pfn < 100 && vfn < 10; pfn++) {
            avcodec.AVPacket packet = grabPacket();
            if (packet != null && packet.size() > 0) {
                hasVideo = true;
                vfn++;
            }
            probePackets.add(packet);
        }
        started = true;
    }

    public boolean nextPacket() throws Exception {
        if (!started) {
            throw new IllegalStateException();
        }
        avcodec.AVPacket packet;
        if (!probePackets.isEmpty()) {
            packet = probePackets.remove(0);
            if (probePackets.isEmpty()) {
                System.gc();
            }
        } else {
            packet = grabPacket();
        }
        /*  Retry connecting if packet returned null */
        if (packet == null) {
            for (int vfn = 0; vfn <15 && started; vfn++) {
                LOG.log(Level.WARNING,"Connection Lost ! Trying to reconnect ...");
                packet = grabPacket();
                if (packet != null) {
                    LOG.log(Level.FINER,"Connected.");
                    break;
                }
            }
            if (packet == null) return false;
        }
        recordPacket(packet);
        return true;
    }

    private avcodec.AVPacket grabPacket() throws Exception {
        if (grabber == null) {
            grabber = grabberSupplier.get();
            grabber.start();
            begin = System.currentTimeMillis() * 1000;
        }

        avcodec.AVPacket packet = grabber.grabPacket();
        return packet;
    }

    private void recordPacket(avcodec.AVPacket packet) throws Exception {
        chunkDetector.nextPacket(System.currentTimeMillis()*1000);
        recorder.recordPacket(packet);
    }

    public Consumer<File> getChunkHandler() {
        return chunkHandler;
    }

    public void setChunkHandler(Consumer<File> chunkHandler) {
        this.chunkHandler = chunkHandler;
    }

}

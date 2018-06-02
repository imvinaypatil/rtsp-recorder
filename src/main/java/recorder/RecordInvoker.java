package recorder;

import org.bytedeco.javacv.FrameGrabber;
import recorder.channel.Channel;
import recorder.channel.sources.Rtsp;
import recorder.common.Executor;
import recorder.common.FileNameFunstions;
import recorder.common.MediaConverter;
import recorder.engine.FfmpegProbeFactory;
import recorder.engine.FfmpegSamplerEngine;
import recorder.sampler.SampleFactory;
import recorder.sampler.Sampler;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RecordInvoker {

    public enum TYPE {
        ALWAYS, MOTION, TIMING, EMERGENCY, ALARM, MISC
    }
    private long firstRecordTimestamp = 0;
    private final Device deviceInfo;
    private final File file;
    private final Sampler sampler;
    private TYPE type;

//    private ExecutorService workerExecutor = Executors.newSingleThreadExecutor();

    private static Logger LOG = Logger.getLogger(RecordInvoker.class.getName());

    public RecordInvoker(Device deviceInfo, File file, TYPE type) {
        Objects.requireNonNull(deviceInfo);
        Objects.requireNonNull(file);
        this.deviceInfo = deviceInfo;
        this.file = file;
        if ( type != null ) {
            this.type = type;
        }
        else this.type = TYPE.MISC;
        if(!this.file.exists()) {
            try {
                throw new FrameGrabber.Exception(" No Dir Exist !");
            } catch (FrameGrabber.Exception e) {
                e.printStackTrace();
            }
        }

        Rtsp sampleRtsp = new Rtsp(deviceInfo.getMEDIA_TYPE(),deviceInfo.getsourceRtsp());
        Channel channel = new Channel(sampleRtsp,deviceInfo.getAUDIO_SRC(),deviceInfo.getRtspTransport());
        FfmpegSamplerEngine ffmpegSamplerEngine = new FfmpegSamplerEngine(channel, this.file);
        FfmpegProbeFactory ffmpegProbeFactory = new FfmpegProbeFactory();
        sampler = new Sampler(ffmpegSamplerEngine,new SampleFactory(ffmpegProbeFactory));

        sampler.getListeners().add(new Executor.Listener() {
            @Override
            public void onStart(Executor executor) {
                deviceInfo.getSamplerListener().onStart(executor,type);
            }

            @Override
            public void onStop(Executor executor) {
                deviceInfo.getSamplerListener().onStop(executor,type,file);
            }

            @Override
            public void onStoping(Executor executor) {
                deviceInfo.getSamplerListener().onStoping(executor,type);
            }

            @Override
            public void onCrash(Executor executor) {
                deviceInfo.getSamplerListener().onCrash(executor,type);
            }
        });

        sampler.setSampleHandler(sample -> {
            /*
             * temporary  JUGGAD for making playback compatible mp4
             *  TODO to be replaced with future javacv version 1.4.2 + fixing ffmpegRecorder (if Recorder bug is fixed)
             */
                LOG.log(Level.FINER, " Initiating  MUXXer");
            Path VIDEO_AVI = Paths.get(sample.getFile().getAbsolutePath());
            String pattern = "yyyy-MM-dd";
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
            String date = simpleDateFormat.format(sample.getBegin());
            File dir = new File(deviceInfo.getDIR()+"\\"+date+"\\"+deviceInfo.getName());
            if (!dir.exists()) dir.mkdirs();
            Path OUTPUT_MP4 = Paths.get(dir.getAbsolutePath(), ("Camera+"+deviceInfo.getName()+"_"+FileNameFunstions.withoutExtension(sample.getFile().getName())+"_"+this.type) + ".mp4");
//            Future pathFuture= workerExecutor.submit(() -> {
            if (!OUTPUT_MP4.toFile().exists()) {
                try {
                    MediaConverter mediaConverter = new MediaConverter();
                    mediaConverter.convertToMp4(VIDEO_AVI,OUTPUT_MP4);
                } catch (Exception e) {
                    e.printStackTrace();
                }finally {
                    LOG.log(Level.FINER, "Removing old file ...");
                    if (!sample.getFile().delete()) {
                        LOG.log(Level.WARNING,"Couldn't delete the raw sample file");
                    }
                }
            }
        });
    }

    public void startSamplerEngine() throws Exception {
        if(!this.file.exists()) {
            throw new FrameGrabber.Exception(" No Dir Exist !");
        }
        LOG.log(Level.INFO,"Recording Started ++ "+deviceInfo.getsourceRtsp());
        sampler.startPacket();
    }

    public void stopSamplerEngine() {
        sampler.stop();
        LOG.log(Level.INFO,"Stoping Recording ++ "+deviceInfo.getsourceRtsp());
    }

    public long getFirstRecordTimestamp() {
        return firstRecordTimestamp;
    }
}

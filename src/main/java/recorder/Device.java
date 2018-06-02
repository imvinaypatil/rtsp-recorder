package recorder;

import org.apache.commons.io.FileUtils;
import recorder.channel.Source;
import recorder.common.Executor;
import recorder.common.FileNameFunstions;
import recorder.common.MediaConverter;
import recorder.common.MediaType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Device extends SimpleDeviceInfo{

    public enum Transport {
        UDP,TCP
    }

    private String name;
    private Transport transport;
    private final String DIR;
    private final Map<RecordInvoker.TYPE, RecordInvoker> activeRecordsMap = new ConcurrentHashMap<>();
    private final ISamplerListener samplerListener;
    private List<IRecordListener> listeners = new ArrayList<>();
    private ExecutorService executorService = Executors.newCachedThreadPool();

    private Logger LOG = Logger.getLogger(Device.class.getName());

    public Device (String name, String url, Source audioSrc, String rtsp_transport, String targetDir) {
        super(url,MediaType.VIDEO,audioSrc);
        this.name = name;
        DIR = targetDir;
        if (rtsp_transport != null && rtsp_transport.equalsIgnoreCase("TCP")) transport = Transport.TCP;
        else transport = Transport.UDP;

        samplerListener = new ISamplerListener() {
            @Override
            public void onStart(Executor executor, RecordInvoker.TYPE type) {
                /* Activating the listener */
                listeners.forEach((IRecordListener recordListener) -> recordListener.onRecord(type));
            }

            @Override
            public void onStop(Executor executor, RecordInvoker.TYPE type,File dir) {
                synchronized (activeRecordsMap) {
                    activeRecordsMap.remove(type);
                    LOG.log(Level.WARNING,"++"+name+" "+type+"++ Recording has been terminated.");
                }
                /* Activating the listener */
                listeners.forEach((IRecordListener recordListener) -> recordListener.onStop(type));
                /*
                 * Convert the last leftover temp files and deletes the redundant files.
                 */
                final File[] files = dir.listFiles();

                if (files != null) {
                    for (File file : files) {
                        Path VIDEO_AVI = Paths.get(file.getAbsolutePath());
                        String pattern = "yyyy-MM-dd";
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
                        String date = simpleDateFormat.format(Long.valueOf(FileNameFunstions.withoutExtension(file.getName()))/1000);
                        File deviceDir = new File(DIR+"\\"+date+"\\"+name);
                        if (!deviceDir.exists()) deviceDir.mkdirs();
                        Path OUTPUT_MP4 = Paths.get(deviceDir.getAbsolutePath(), ("Camera-"+name+"_"+FileNameFunstions.withoutExtension(file.getName())+"_"+type) + ".mp4");
                        /* Convert the file iff ( !outputFileExist) & (inputFile.length > 8 Kb --> so the file isn't corrupt) */
                        if (!OUTPUT_MP4.toFile().exists()  && file.length() > 8192) {
                            MediaConverter mediaConverter = new MediaConverter();
                            try {
                                mediaConverter.convertToMp4(VIDEO_AVI,OUTPUT_MP4);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }finally {
                                /* Delete the tempDir */
                                try {
                                    FileUtils.deleteDirectory(dir);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
            }

            @Override
            public void onStoping(Executor executor, RecordInvoker.TYPE type) { }

            @Override
            public void onCrash(Executor executor, RecordInvoker.TYPE type) {
                LOG.log(Level.WARNING,"++ "+name +" ++Record Engine crashed due to "+ executor.getLastCrashException().getMessage() +"\n");
                synchronized (activeRecordsMap) {
                    activeRecordsMap.get(type).stopSamplerEngine();
                    activeRecordsMap.remove(type);
                }
                //TODO undecided What to do on Crash ?
                /* Activating the listener */
                listeners.forEach((IRecordListener recordListener) -> recordListener.onCrash(type));
            }
        };
    }

    public Boolean isAlwaysRecordingEnabled() {
        synchronized (activeRecordsMap) {
            return activeRecordsMap.containsKey(RecordInvoker.TYPE.ALWAYS);
        }
    }

    public Boolean isEmergencyRecordingEnabled() {
        synchronized (activeRecordsMap) {
            return activeRecordsMap.containsKey(RecordInvoker.TYPE.EMERGENCY);
        }
    }

    public Boolean isAnyRecording () {
        synchronized (activeRecordsMap) {
            return activeRecordsMap.size() > 0;
        }
    }

    public void stopAllRecordings() {
        synchronized (activeRecordsMap) {
            executorService.submit(() -> {
                activeRecordsMap.entrySet().parallelStream().forEach(entry -> {
                    try {
                        entry.getValue().stopSamplerEngine();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    finally {
                        activeRecordsMap.remove(entry.getKey());
                    }
                });
            });
        }
    }

    public List<RecordInvoker.TYPE> getActiveRecordList() {
        synchronized (activeRecordsMap) {
            return new ArrayList<>(activeRecordsMap.keySet());
        }
    }

    /* Factory Method to return new recorder.RecordInvoker*/
    private RecordInvoker initRecording (RecordInvoker.TYPE type) {
        File file = new File("cache/"+name);
        if (!file.exists()) {
            file.mkdirs();
        }
        return new RecordInvoker(this,file,type);
    }

    /*
    * @param trigger
    * @return   true if (trigger) & (!activeRecordMap.TYPE)
    *                true if (!trigger) & (activeRecordMap.TYPE)
    *                false if (trigger) & (activeRecordMap.TYPE)
    *               false if (!trigger) & (!activeRecordMap.TYPE) */
    public boolean triggerRecording(Boolean trigger, RecordInvoker.TYPE type) throws Exception {
        synchronized (activeRecordsMap) {
            if (trigger) {
                if (activeRecordsMap.containsKey(type)) {
                    return false;
                } else {
                    RecordInvoker recordInvoker = initRecording(type);
                    activeRecordsMap.put(type, recordInvoker);
                    executorService.submit(() -> {
                        try {
                            recordInvoker.startSamplerEngine();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                    return true;
                }
            }
            else {
                if (activeRecordsMap.containsKey(type)) {
                    executorService.submit(() -> activeRecordsMap.get(type).stopSamplerEngine());
                    return true;
                } else {
                    return false;
                }
            }
        }
    }

    public String getName() {
        return name;
    }

    public Transport getRtspTransport() {
        return transport;
    }

    public ISamplerListener getSamplerListener() {
        return samplerListener;
    }

    public void addListener(IRecordListener listener) {
        listeners.add(listener);
    }

    public void removeListener(IRecordListener listener) {
        listeners.remove(listener);
    }

    public void  clearListeners() {
        listeners.clear();
    }

    public String getDIR() {
        return DIR;
    }

}
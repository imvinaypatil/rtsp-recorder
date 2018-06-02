package recorder.sampler;

import recorder.common.MediaType;

import java.io.File;

public abstract class Probe {

    private final File mediaFile;
    private boolean inited = false;
    protected Long duration = null;
    protected MediaType mediaType = null;

    protected Probe(File mediaFile) {
        if (mediaFile == null) {
            throw new IllegalArgumentException();
        }
        if (!mediaFile.exists()) {
            throw new RuntimeException(String.format("File no found \"%s\".", mediaFile.getAbsolutePath()));
        }
        this.mediaFile = mediaFile;
    }

    private void init() {
        if (!inited) {
            initFileInfo();
        }
        inited = true;
    }

    protected abstract void initFileInfo();

    public File getMediaFile() {
        return mediaFile;
    }

    /**
     * @return Duration of sample in milliseconds.
     */
    public long getDuration() {
        if (!inited) {
            init();
        }
        return duration;
    }

    public MediaType getMediaType() {
        if (!inited) {
            init();
        }
        return mediaType;
    }
}

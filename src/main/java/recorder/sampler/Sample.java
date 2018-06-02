package recorder.sampler;

import recorder.common.HasMediaType;
import recorder.common.MediaType;

import java.io.File;
import java.util.Date;

public class Sample implements HasMediaType {

    private String samplerInfo;
    private final Date begin;
    private final String extension;
    private final File file;

    private final int dudation;

    private int size;
    private final MediaType mediaType;
    private final Date end;

    public Sample(
            String samplerInfo,
            Date begin,
            String extension,
            File file,
            int duration,
            int size,
            MediaType mediaType
    ) {
        if (samplerInfo == null
                || begin == null
                || extension == null
                || file == null
                || duration <= 0
                || size <= 0
                || mediaType == null) {
            throw new IllegalArgumentException();
        }
        if (!file.exists()) {
            throw new IllegalArgumentException(String.format("File \"%s\" doesn't exist.", file.getAbsolutePath()));
        }
        if (samplerInfo.isEmpty()) {
            throw new IllegalArgumentException("Sampler info is empty.");
        }

        this.samplerInfo = samplerInfo;
        this.begin = begin;
        this.extension = extension;
        this.file = file;
        this.dudation = duration;
        this.size = size;
        this.mediaType = mediaType;
        this.end = new Date(begin.getTime() + duration);
    }

    public String getSamplerInfo() {
        return samplerInfo;
    }

    public String getSamplerVersion() {
        int separatorIndex = samplerInfo.indexOf("/");
        if (separatorIndex != -1) {
            return samplerInfo.substring(0, separatorIndex);
        }
        return samplerInfo;
    }

    public Date getBegin() {
        return begin;
    }

    public String getExtension() {
        return extension;
    }

    public File getFile() {
        return file;
    }

    public int getDudation() {
        return dudation;
    }

    public int getSize() {
        return size;
    }

    @Override
    public MediaType getMediaType() {
        return mediaType;
    }

    public Date getEnd() {
        return end;
    }

}

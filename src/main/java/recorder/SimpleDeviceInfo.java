package recorder;

import recorder.channel.Source;
import recorder.common.MediaType;

import java.util.Objects;

public class SimpleDeviceInfo {

    private final String SOURCE_RTSP;
    private final Source AUDIO_SRC;
    private final MediaType MEDIA_TYPE;

    public SimpleDeviceInfo(String SOURCE_RTSP, MediaType media_type, Source audio_src) {
        Objects.requireNonNull(SOURCE_RTSP);
        Objects.requireNonNull(media_type);
        AUDIO_SRC = audio_src;
        MEDIA_TYPE = media_type;
        this.SOURCE_RTSP = SOURCE_RTSP;
    }

    public String getsourceRtsp() {
        return SOURCE_RTSP;
    }

    public Source getAUDIO_SRC() {
        return AUDIO_SRC;
    }

    public MediaType getMEDIA_TYPE() {
        return MEDIA_TYPE;
    }

}

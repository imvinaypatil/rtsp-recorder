package recorder.channel.sources;

import recorder.channel.Source;
import recorder.common.MediaType;

public class Rtsp extends Source {

    private MediaType mediaType;

    private String uri;

    public Rtsp(MediaType mediaType, String uri) {
        if (mediaType == null || uri == null) {
            throw new IllegalArgumentException();
        }
        this.mediaType = mediaType;
        this.uri = uri;
    }

    @Override
    public MediaType getMediaType() {
        return mediaType;
    }

    public String getUri() {
        return uri;
    }

}

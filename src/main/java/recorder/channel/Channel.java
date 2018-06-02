package recorder.channel;

import recorder.Device;
import recorder.common.HasMediaType;
import recorder.common.MediaType;

public class Channel implements HasMediaType {

    private final Source videoSource;
    private final Source audioSource;
    private final Device.Transport transport;

    public Channel(Source videoSource, Source audioSource, Device.Transport transport) {
        if (videoSource == null && audioSource == null) {
            throw new IllegalArgumentException("Channel should have at lest one source.");
        }
        if (videoSource != null && !videoSource.getMediaType().isCompatible(MediaType.VIDEO)) {
            throw new IllegalArgumentException("Type of videoSource is not compatible with VIDEO type.");
        }
        if (audioSource != null && !audioSource.getMediaType().isCompatible(MediaType.AUDIO)) {
            throw new IllegalArgumentException("Type of audioSource is not compatible with AUDIO type.");
        }
        this.videoSource = videoSource;
        this.audioSource = audioSource;
        this.transport = transport;
    }

    public Source getVideoSource() {
        return videoSource;
    }

    public Source getAudioSource() {
        return audioSource;
    }

    @Override
    public MediaType getMediaType() {
        if (videoSource == null) {
            return MediaType.AUDIO;
        } else if (audioSource == null) {
            return MediaType.VIDEO;
        } else {
            return MediaType.VIDEO_AND_AUDIO;
        }
    }

    public Device.Transport getTransport() {
        return transport;
    }
}

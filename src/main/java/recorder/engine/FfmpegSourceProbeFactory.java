package recorder.engine;


import recorder.channel.Source;
import recorder.channel.SourceProbe;
import recorder.channel.SourceProbeFactory;


public class FfmpegSourceProbeFactory implements SourceProbeFactory {

    @Override
    public SourceProbe createProbe(Source source) {
        return new FfmpegSourceProbe(source);
    }

}

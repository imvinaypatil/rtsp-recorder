package recorder.engine;


import recorder.sampler.Probe;
import recorder.sampler.ProbeFactory;

import java.io.File;


public class FfmpegProbeFactory implements ProbeFactory {

    @Override
    public Probe createProbe(File mediFile) {
        return new FfmpegProbe(mediFile);
    }
    
}

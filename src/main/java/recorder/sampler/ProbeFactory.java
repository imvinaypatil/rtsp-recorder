package recorder.sampler;

import java.io.File;


public interface ProbeFactory {
    public Probe createProbe(File mediFile);
}

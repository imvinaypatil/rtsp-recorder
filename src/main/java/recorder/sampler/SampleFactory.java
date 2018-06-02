package recorder.sampler;

import recorder.common.FileNameFunstions;

import java.io.File;
import java.util.Date;

public class SampleFactory {

    private ProbeFactory probeFactory;

    public SampleFactory(ProbeFactory probeFactory) {
        if (probeFactory == null) {
            throw new IllegalArgumentException();
        }
        this.probeFactory = probeFactory;
    }

    public Sample createSample(Sampler sampler,
                               Date begin,
                               File file
    ) {
        if (sampler == null
                || begin == null
                || file == null) {
            throw new IllegalArgumentException();
        }
        if (!file.exists()) {
            throw new IllegalArgumentException("File doesn't exist.");
        }

        String extension = FileNameFunstions.extension(file.getName());
        if (extension.isEmpty()) {
            throw new RuntimeException("Media file extension is empty.");
        }

        Probe probe = probeFactory.createProbe(file);

        try {
            return new Sample(sampler.getInfo(), begin, extension, file, (int) probe.getDuration(), (int) file.length(), probe.getMediaType());
        } catch (Exception ex) {
            throw new RuntimeException("Can't read sample info.", ex);
        }
    }
}

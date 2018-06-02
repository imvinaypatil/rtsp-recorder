package recorder.common;

import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.UrlInput;
import com.github.kokorin.jaffree.ffmpeg.UrlOutput;

import java.nio.file.Path;
import java.nio.file.Paths;

public class MediaConverter {

    public MediaConverter() {};

    public Path convertToMp4 (Path input,Path output) {
        Path BIN = Paths.get(System.getProperty("user.dir")+"/lib/ffmpeg");
        FFmpeg ffmpeg = FFmpeg.atPath(BIN)
                .addInput(UrlInput.fromPath(input))
                .addOutput(UrlOutput.toPath(output)
                        .addArguments("-vcodec", "copy")
                        .addArguments("-threads", "10")
                        .addArguments("-acodec", "copy")
                );
        ffmpeg.execute();
        return output;
    }
}

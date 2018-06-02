package recorder.common.tmp_file_marker;

import recorder.common.FileNameFunstions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;
import java.util.function.Predicate;

public class TmpMarkers {

    public final static UUID SESSION_UUID = UUID.randomUUID();
    public final static String FLAG_POSTFIX = ".ptmp";

    private TmpMarkers() {
    }

    public static void cleanupFile(File file, Predicate<File> vouter) {
        if (file == null
                || vouter == null) {
            throw new NullPointerException();
        }
        TmpMarker possibleTmpFile = new TmpMarker(file);
        TmpMarkerFlag flag = possibleTmpFile.readFlag();
        if (flag != null && !flag.getSessionUuid().equals(SESSION_UUID)) {
            if (!Files.exists(file.toPath()) || vouter.test(file)) {
                possibleTmpFile.deleteFlag();
            } else {
                try {
                    Files.delete(file.toPath());
                } catch (IOException ex) {
                    throw new TmpMarkerException(String.format("Can't delete possible tmp file: %s.", file.getAbsolutePath()), ex);
                }
                possibleTmpFile.deleteFlag();
            }
        }
    }

    public static void cleanupDirectory(File dir, Predicate<File> vouter) {
        if (dir == null
                || vouter == null) {
            throw new NullPointerException();
        }
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException(String.format("The dir should be a directory not a file.", dir.getAbsolutePath()));
        }
        File[] flagFiles = dir.listFiles(file -> file.getName().endsWith(FLAG_POSTFIX));
        for (File flagFile : flagFiles) {
            File file = new File(FileNameFunstions.withoutExtension(flagFile.getAbsolutePath()));
            cleanupFile(file, vouter);
        }
    }
}

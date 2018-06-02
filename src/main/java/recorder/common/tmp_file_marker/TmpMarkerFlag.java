package recorder.common.tmp_file_marker;

import java.io.Serializable;
import java.util.UUID;

public class TmpMarkerFlag implements Serializable {

    private final UUID sessionUuid = TmpMarkers.SESSION_UUID;

    public UUID getSessionUuid() {
        return sessionUuid;
    }
}

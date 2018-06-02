package recorder.engine.stream_chunker;

public interface ChunkListener {

    default public void onChunkEnd() throws Exception {
    }

    default public void onChunkBegin() throws Exception {

    }
}

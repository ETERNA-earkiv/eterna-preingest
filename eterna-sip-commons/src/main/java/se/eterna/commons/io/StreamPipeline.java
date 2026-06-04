package se.eterna.commons.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Läser en InputStream och skriver data genom en {@link StreamProcessor}-pipeline till en OutputStream.
 */
public class StreamPipeline {

    private static final int BUFFER_SIZE = 8192;

    private final byte[] buffer = new byte[BUFFER_SIZE];

    public void process(InputStream in, OutputStream out, StreamProcessor processor)
            throws IOException {
        try (OutputStream wrapped = processor.wrap(out)) {
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                wrapped.write(buffer, 0, bytesRead);
            }
            wrapped.flush();
        }
    }
}

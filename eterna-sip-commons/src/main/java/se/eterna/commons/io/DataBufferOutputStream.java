package se.eterna.commons.io;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import reactor.core.publisher.FluxSink;

import java.io.IOException;
import java.io.OutputStream;

/**
 * OutputStream som emitterar data som reaktiva DataBuffer-chunks till en FluxSink.
 * Buffrar internt i 64 KB-bitar och flushar varje chunk till sinken.
 */
public class DataBufferOutputStream extends OutputStream {

    private static final int DEFAULT_CHUNK_SIZE = 64 * 1024;

    private final DataBufferFactory bufferFactory;
    private final int chunkSize;
    private FluxSink<DataBuffer> sink;
    private DataBuffer currentBuffer;
    private int position = 0;
    private boolean closed = false;

    public DataBufferOutputStream(DataBufferFactory bufferFactory, FluxSink<DataBuffer> sink) {
        this.bufferFactory = bufferFactory;
        this.sink = sink;
        this.chunkSize = DEFAULT_CHUNK_SIZE;
        this.currentBuffer = bufferFactory.allocateBuffer(chunkSize);
    }

    public DataBufferOutputStream(DataBufferFactory bufferFactory) {
        this.bufferFactory = bufferFactory;
        this.chunkSize = DEFAULT_CHUNK_SIZE;
        this.currentBuffer = bufferFactory.allocateBuffer(chunkSize);
    }

    public void setSink(FluxSink<DataBuffer> sink) {
        this.sink = sink;
    }

    @Override
    public void write(int b) throws IOException {
        ensureCapacity(1);
        currentBuffer.write((byte) b);
        position++;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        int remaining = len;
        int offset = off;
        while (remaining > 0) {
            int writable = Math.min(chunkSize - position, remaining);
            ensureCapacity(writable);
            currentBuffer.write(b, offset, writable);
            position += writable;
            remaining -= writable;
            offset += writable;
        }
    }

    private void ensureCapacity(int needed) {
        if (position + needed > chunkSize) {
            flushCurrent();
        }
    }

    private void flushCurrent() {
        if (sink == null) {
            throw new IllegalStateException("DataBufferOutputStream: sink är inte satt");
        }
        if (position > 0) {
            sink.next(currentBuffer);
            currentBuffer = bufferFactory.allocateBuffer(chunkSize);
            position = 0;
        }
    }

    @Override
    public void flush() {
        flushCurrent();
    }

    @Override
    public void close() {
        if (closed) return;
        flushCurrent();
        DataBufferUtils.release(currentBuffer);
        closed = true;
    }
}

package se.eterna.commons.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Blockerande InputStream som matas inkrementellt med ByteBuffers från en producenttråd.
 * Trådsäker: producenttråden matar via {@link #feed} och konsumenttråden blockerar i {@link #read}.
 */
public final class DataBufferInputStream extends InputStream {

    private final Queue<ByteBuffer> buffers = new ArrayDeque<>();
    private boolean closed;
    private IOException failure;

    @Override
    public synchronized int read() throws IOException {
        byte[] one = new byte[1];
        int r = read(one, 0, 1);
        return r == -1 ? -1 : one[0] & 0xFF;
    }

    @Override
    public synchronized int read(byte[] dst, int off, int len) throws IOException {
        while (buffers.isEmpty()) {
            if (failure != null) throw failure;
            if (closed) return -1;
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Avbruten", e);
            }
        }

        ByteBuffer buf = buffers.peek();
        int toRead = Math.min(len, buf.remaining());
        buf.get(dst, off, toRead);

        if (!buf.hasRemaining()) {
            buffers.poll();
        }
        return toRead;
    }

    /** Mata in ett ByteBuffer — anropas från producenttråden. */
    public synchronized void feed(ByteBuffer buffer) {
        buffers.add(buffer);
        notifyAll();
    }

    /** Signalera att all data har matats in. */
    public synchronized void closeInput() {
        closed = true;
        notifyAll();
    }

    /** Signalera att ett fel inträffade — nästa läsning kastar detta. */
    public synchronized void fail(IOException ex) {
        failure = ex;
        notifyAll();
    }
}

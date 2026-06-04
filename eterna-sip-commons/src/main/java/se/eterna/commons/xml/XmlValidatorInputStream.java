package se.eterna.commons.xml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Intern InputStream för StaxXmlValidator.
 * Trådsäker blockerande implementation — producenten matar via {@link #feed},
 * konsumenten (StAX-parsern) läser via {@link #read}.
 */
final class XmlValidatorInputStream extends InputStream {

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
        if (!buf.hasRemaining()) buffers.poll();
        return toRead;
    }

    synchronized void feed(ByteBuffer buffer) {
        buffers.add(buffer);
        notifyAll();
    }

    synchronized void closeInput() {
        closed = true;
        notifyAll();
    }

    synchronized void fail(IOException ex) {
        failure = ex;
        notifyAll();
    }
}

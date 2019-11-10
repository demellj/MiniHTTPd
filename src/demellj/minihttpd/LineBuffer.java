package demellj.minihttpd;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

public class LineBuffer implements Iterable<String> {
    public static int BUFFER_SIZE = 1024;

    final ByteBuffer buffer;
    final CharBuffer charBuffer;

    final CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();

    final ConcurrentLinkedQueue<String> lines = new ConcurrentLinkedQueue<>();
    final StringBuilder lineBuffer = new StringBuilder();

    public LineBuffer() {
        this.buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
        this.charBuffer = CharBuffer.allocate(BUFFER_SIZE);
    }

    public synchronized int read(ReadableByteChannel in) throws IOException {
        var totalBytesRead = 0;

        while (true) {
            buffer.clear();
            final var bytesRead = in.read(buffer);

            if (bytesRead < 0)
                return -1;

            if (bytesRead == 0)
                break;

            buffer.flip();

            totalBytesRead += bytesRead;

            charBuffer.clear();
            CoderResult result = null;
            while (true) {
                result = decoder.decode(buffer, charBuffer, false);

                if (result.isOverflow()) {
                    drainCharBuffer();
                } else {
                    break;
                }
            }

            if (result.isUnderflow()) {
                drainCharBuffer();
            } else { // error
                charBuffer.clear();
            }

            decoder.decode(buffer, charBuffer, true);
            decoder.flush(charBuffer);

            drainCharBuffer();

            decoder.reset();
        }

        return totalBytesRead;
    }

    private void drainCharBuffer() {
        charBuffer.flip();
        if (charBuffer.hasRemaining()) {
            while (charBuffer.hasRemaining()) {
                final char ch = charBuffer.get();

                if (ch == '\r') continue;

                if (ch == '\n') {
                    lines.add(lineBuffer.toString());
                    lineBuffer.delete(0, lineBuffer.length());
                } else {
                    lineBuffer.append(ch);
                }
            }
            charBuffer.flip();
        }
    }

    public synchronized void flush() {
        if (lineBuffer.length() > 0) {
            lines.add(lineBuffer.toString());
            lineBuffer.delete(0, lineBuffer.length());
        }
    }

    public String nextLine() {
        return lines.poll();
    }

    @Override
    public Iterator<String> iterator() {
        return lines.iterator();
    }
}

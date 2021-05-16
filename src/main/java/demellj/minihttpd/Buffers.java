package demellj.minihttpd;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;

public class Buffers {
    public final static int BUFFER_SIZE = 1024;

    private final CharBuffer charBuffer = CharBuffer.allocate(BUFFER_SIZE);
    private final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
    private final LineBuffer lineBuffer = new LineBuffer();

    /**
     * Transfer chars from the charBuffer to the byteBuffer and return the number
     * of chars remaining.
     */
    public int transferToByteBuffer(CharsetEncoder encoder, boolean endOfInput) {
        final int charsRemaining = charBuffer.remaining();

        if (charsRemaining <= 0)
            return 0;

        CoderResult result;
        if (endOfInput) {
            result = encoder.encode(charBuffer, byteBuffer, true);
            encoder.flush(byteBuffer);
            encoder.reset();
        } else {
            result = encoder.encode(charBuffer, byteBuffer, false);
        }

        if (result.isError())
            return -1;
        else
            return charBuffer.remaining();
    }

    /**
     * Transfer bytes from the byteBuffer to the charBuffer and return the number
     * of bytes remaining.
     */
    public int transferToCharBuffer(CharsetDecoder decoder, boolean endOfInput) {
        final int bytesRemaining = byteBuffer.remaining();

        if (bytesRemaining <= 0)
            return 0;

        CoderResult result;
        if (endOfInput) {
            result = decoder.decode(byteBuffer, charBuffer, true);
            decoder.flush(charBuffer);
            decoder.reset();
        } else {
            result = decoder.decode(byteBuffer, charBuffer, false);
        }

        if (result.isError())
            return -1;
        else
            return byteBuffer.remaining();
    }

    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }

    public CharBuffer getCharBuffer() {
        return charBuffer;
    }

    public LineBuffer getLineBuffer() {
        return lineBuffer;
    }

    public void clear() {
        byteBuffer.clear();
        charBuffer.clear();
    }
}

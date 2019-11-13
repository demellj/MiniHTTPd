package demellj.minihttpd.response;

import demellj.minihttpd.Buffers;
import demellj.minihttpd.Client;
import demellj.minihttpd.TextStreamWriter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;

public class ChunkedTextStreamWriterImpl implements TextStreamWriter {
    final private CharsetEncoder encoder;
    final private ByteBuffer chunkBuffer;
    final private Client client;
    final private BuilderState state;

    public ChunkedTextStreamWriterImpl(Client client, BuilderState state, Charset charset) {
        this.client = client;
        this.state = state;

        encoder = charset.newEncoder();

        // chunkBuffer must be at least two bytes long
        chunkBuffer = ByteBuffer.allocate(256);
    }

    @Override
    public TextStreamWriter write(CharBuffer buffer) throws IOException, IllegalBuilderStateException {
        if (!state.inBody())
            throw new IllegalBuilderStateException(BuilderState.State.Body, state);

        if (!state.inBody()) return null;
        writeChunkedChars(buffer);
        return this;
    }

    @Override
    public TextStreamWriter write(CharSequence data) throws IOException, IllegalBuilderStateException {
        if (!state.inBody())
            throw new IllegalBuilderStateException(BuilderState.State.Body, state);

        if (!state.inBody()) return null;
        writeChunkedChars(CharBuffer.wrap(data));
        return this;
    }

    @Override
    public TextStreamWriter write(char[] data) throws IOException, IllegalBuilderStateException {
        if (!state.inBody())
            throw new IllegalBuilderStateException(BuilderState.State.Body, state);

        writeChunkedChars(CharBuffer.wrap(data));
        return this;
    }

    @Override
    public TextStreamWriter write(char data) throws IOException, IllegalBuilderStateException {
        if (!state.inBody())
            throw new IllegalBuilderStateException(BuilderState.State.Body, state);

        if (!state.inBody()) return null;
        writeChunkedChars(CharBuffer.wrap(String.valueOf(data)));
        return this;
    }

    @Override
    public void complete() throws IOException, IllegalBuilderStateException {
        if (!state.inBody())
            throw new IllegalBuilderStateException(BuilderState.State.Body, state);

        writeTerminatingTrailer();
        state.complete();
    }

    private void writeChunkedChars(CharBuffer inCharBuffer) throws IOException {
        final SocketChannel channel = client.getChannel();
        final Buffers buffers = client.getBuffers();

        final ByteBuffer byteBuffer = buffers.getByteBuffer();
        byteBuffer.clear();

        CoderResult result = null;

        encoder.reset();
        while (inCharBuffer.hasRemaining()) {
            result = encoder.encode(inCharBuffer, byteBuffer, false);
            if (result.isOverflow()) {
                byteBuffer.flip();
                writeSize(byteBuffer.limit());
                channel.write(byteBuffer);
                writeTrailer();
                byteBuffer.flip();
            } else if (result.isError()) {
                break;
            }
        }

        if (result != null && result.isUnderflow() && byteBuffer.hasRemaining()) {
            byteBuffer.flip();
            writeSize(byteBuffer.limit());
            channel.write(byteBuffer);
            writeTrailer();
        }
    }

    private void writeSize(long numBytes) throws IOException {
        final SocketChannel channel = client.getChannel();
        final Buffers       buffers = client.getBuffers();

        final CharBuffer tmpCharBuffer = buffers.getCharBuffer();
        tmpCharBuffer.clear();
        tmpCharBuffer.put(Long.toHexString(numBytes)).put("\r\n");
        tmpCharBuffer.flip();

        chunkBuffer.clear();

        CoderResult result = null;
        while (tmpCharBuffer.hasRemaining()) {
            result = encoder.encode(tmpCharBuffer, chunkBuffer, false);
            if (result.isOverflow()) {
                chunkBuffer.flip();
                channel.write(chunkBuffer);
                chunkBuffer.flip();
            } else if (result.isError()) {
                break;
            }
        }

        if (result != null && result.isUnderflow() && chunkBuffer.hasRemaining()) {
            chunkBuffer.flip();
            channel.write(chunkBuffer);
        }
    }

    private void writeTrailer() throws IOException {
        final SocketChannel channel = client.getChannel();
        final Buffers       buffers = client.getBuffers();

        final CharBuffer tmpCharBuffer = buffers.getCharBuffer();
        tmpCharBuffer.clear();

        tmpCharBuffer.put("\r\n");
        tmpCharBuffer.flip();

        chunkBuffer.clear();
        encoder.encode(tmpCharBuffer, chunkBuffer, false);
        chunkBuffer.flip();

        channel.write(chunkBuffer);
    }

    private void writeTerminatingTrailer() throws IOException {
        final SocketChannel channel = client.getChannel();
        final Buffers       buffers = client.getBuffers();

        final CharBuffer tmpCharBuffer = buffers.getCharBuffer();
        tmpCharBuffer.clear();

        tmpCharBuffer.put("0\r\n\r\n");
        tmpCharBuffer.flip();

        chunkBuffer.clear();
        encoder.encode(tmpCharBuffer, chunkBuffer, true);
        encoder.flush(chunkBuffer);
        chunkBuffer.flip();

        channel.write(chunkBuffer);
    }
}

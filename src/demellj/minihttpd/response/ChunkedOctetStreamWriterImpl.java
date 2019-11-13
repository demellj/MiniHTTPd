package demellj.minihttpd.response;

import demellj.minihttpd.Buffers;
import demellj.minihttpd.Client;
import demellj.minihttpd.OctetStreamWriter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;

public class ChunkedOctetStreamWriterImpl implements OctetStreamWriter {
    final private Client client;
    final private CharsetEncoder utf8Encoder;
    final private BuilderState state;

    public ChunkedOctetStreamWriterImpl(Client client,
                                        CharsetEncoder utf8Encoder,
                                        BuilderState state) {
        this.client = client;
        this.utf8Encoder = utf8Encoder;
        this.state = state;
    }

    @Override
    public OctetStreamWriter write(ByteBuffer buffer) throws IOException, IllegalBuilderStateException {
        if (!state.inBody())
            throw new IllegalBuilderStateException(BuilderState.State.Body, state);

        writeChunk(buffer);
        return this;
    }

    @Override
    public OctetStreamWriter write(byte[] data) throws IOException, IllegalBuilderStateException {
        if (!state.inBody())
            throw new IllegalBuilderStateException(BuilderState.State.Body, state);

        writeChunk(ByteBuffer.wrap(data));
        return this;
    }

    @Override
    public OctetStreamWriter write(byte data) throws IOException, IllegalBuilderStateException {
        if (!state.inBody())
            throw new IllegalBuilderStateException(BuilderState.State.Body, state);

        final ByteBuffer byteBuffer = client.getBuffers().getByteBuffer();
        byteBuffer.clear();

        byteBuffer.put(data);
        byteBuffer.flip();

        writeChunk(byteBuffer);

        return this;
    }

    @Override
    public void complete() throws IOException, IllegalBuilderStateException {
        if (!state.inBody())
            throw new IllegalBuilderStateException(BuilderState.State.Body, state);

        writeTerminatingTrailer();

        state.complete();
    }

    private void writeChunk(ByteBuffer inByteBuffer) throws IOException {
        writeSize(inByteBuffer.limit());
        client.getChannel().write(inByteBuffer);
        writeTrailer();
    }

    private void writeSize(long numBytes) throws IOException {
        final Buffers buffers = client.getBuffers();
        final SocketChannel channel = client.getChannel();

        final CharBuffer tmpCharBuffer = buffers.getCharBuffer();
        tmpCharBuffer.clear();
        tmpCharBuffer.put(Long.toHexString(numBytes)).put("\r\n");
        tmpCharBuffer.flip();

        final ByteBuffer chunkBuffer = buffers.getByteBuffer();
        chunkBuffer.clear();

        CoderResult result = null;
        while (tmpCharBuffer.hasRemaining()) {
            result = utf8Encoder.encode(tmpCharBuffer, chunkBuffer, false);
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
        final Buffers buffers = client.getBuffers();

        final CharBuffer tmpCharBuffer = buffers.getCharBuffer();
        tmpCharBuffer.clear();
        tmpCharBuffer.put("\r\n");
        tmpCharBuffer.flip();

        final ByteBuffer chunkBuffer = buffers.getByteBuffer();
        chunkBuffer.clear();
        utf8Encoder.encode(tmpCharBuffer, chunkBuffer, false);
        chunkBuffer.flip();

        client.getChannel().write(chunkBuffer);
    }

    private void writeTerminatingTrailer() throws IOException {
        final Buffers buffers = client.getBuffers();

        final CharBuffer tmpCharBuffer = buffers.getCharBuffer();
        tmpCharBuffer.clear();

        tmpCharBuffer.put("0\r\n\r\n");
        tmpCharBuffer.flip();

        final ByteBuffer chunkBuffer = buffers.getByteBuffer();
        chunkBuffer.clear();
        utf8Encoder.encode(tmpCharBuffer, chunkBuffer, true);
        utf8Encoder.flush(chunkBuffer);
        chunkBuffer.flip();

        client.getChannel().write(chunkBuffer);
    }
}

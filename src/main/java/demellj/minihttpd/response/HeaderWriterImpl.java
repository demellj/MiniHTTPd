package demellj.minihttpd.response;

import demellj.minihttpd.Client;
import demellj.minihttpd.HeaderWriter;
import demellj.minihttpd.OctetStreamWriter;
import demellj.minihttpd.TextStreamWriter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

public class HeaderWriterImpl implements HeaderWriter {
    final private Client client;
    final private BuilderState state;
    final private CharsetEncoder utf8Encoder;
    final private TreeMap<String, String> injectedHeaders;

    public HeaderWriterImpl(Client client,
                            BuilderState state,
                            TreeMap<String, String> injectedHeaders,
                            CharsetEncoder utf8Encoder) {
        this.client = client;
        this.state = state;
        this.injectedHeaders = injectedHeaders;
        this.utf8Encoder = utf8Encoder;
    }

    @Override
    public HeaderWriter writeContentType(String format) throws IOException, IllegalBuilderStateException {
        if (!state.inHeaders())
            throw new IllegalBuilderStateException(BuilderState.State.Headers, state);

        return writeHeader("Content-Type", format);
    }

    @Override
    public HeaderWriter writeContentLength(long numBytes) throws IOException, IllegalBuilderStateException {
        if (!state.inHeaders())
            throw new IllegalBuilderStateException(BuilderState.State.Headers, state);

        return writeHeader("Content-Length", Long.toString(numBytes));
    }

    @Override
    public HeaderWriter writeHeader(CharSequence key, CharSequence value) throws IOException, IllegalBuilderStateException {
        if (!state.inHeaders())
            throw new IllegalBuilderStateException(BuilderState.State.Headers, state);

        return writeHeader(key, CharBuffer.wrap(value));
    }

    @Override
    public HeaderWriter writeHeader(CharSequence key, CharBuffer valueBuffer)
            throws IOException, IllegalBuilderStateException {

        if (!state.inHeaders())
            throw new IllegalBuilderStateException(BuilderState.State.Headers, state);

        client.writeChars(CharBuffer.wrap(key), utf8Encoder);
        client.writeChars(CharBuffer.wrap(": "), utf8Encoder);
        client.writeChars(valueBuffer, utf8Encoder);
        client.writeChars(CharBuffer.wrap("\r\n"), utf8Encoder);

        return this;
    }

    @Override
    public TextStreamWriter textBody(Charset charset, long numBytes) throws IOException, IllegalBuilderStateException {
        if (!state.inHeaders())
            throw new IllegalBuilderStateException(BuilderState.State.Headers, state);

        completeHeaders();
        writeHeader("Content-Length", String.valueOf(numBytes));
        client.writeChars(CharBuffer.wrap("\r\n"), utf8Encoder);

        return new TextStreamWriterImpl(client, state, charset);
    }

    @Override
    public TextStreamWriter textBody(Charset charset) throws IOException, IllegalBuilderStateException {
        if (!state.inHeaders())
            throw new IllegalBuilderStateException(BuilderState.State.Headers, state);

        completeHeaders();

        return new BufferingTextStreamWriterImpl(client, utf8Encoder, state, charset);
    }

    @Override
    public TextStreamWriter textBodyChunked(Charset charset) throws IOException, IllegalBuilderStateException {
        if (!state.inHeaders())
            throw new IllegalBuilderStateException(BuilderState.State.Headers, state);

        completeHeaders();
        writeHeader("Transfer-Encoding", "chunked");
        client.writeChars(CharBuffer.wrap("\r\n"), utf8Encoder);

        return new ChunkedTextStreamWriterImpl(client, state, charset);
    }

    @Override
    public OctetStreamWriter octetStreamBody(long numBytes) throws IOException, IllegalBuilderStateException {
        if (!state.inHeaders())
            throw new IllegalBuilderStateException(BuilderState.State.Headers, state);

        completeHeaders();
        writeHeader("Content-Length", String.valueOf(numBytes));
        client.writeChars(CharBuffer.wrap("\r\n"), utf8Encoder);

        return new OctetStreamWriterImpl(client, state);
    }

    @Override
    public OctetStreamWriter octetStreamBody() throws IOException, IllegalBuilderStateException {
        if (!state.inHeaders())
            throw new IllegalBuilderStateException(BuilderState.State.Headers, state);

        completeHeaders();

        return new BufferingOctetStreamWriterImpl(client, utf8Encoder, state);
    }

    @Override
    public OctetStreamWriter octetStreamBodyChunked() throws IOException, IllegalBuilderStateException {
        if (!state.inHeaders())
            throw new IllegalBuilderStateException(BuilderState.State.Headers, state);

        completeHeaders();
        writeHeader("Transfer-Encoding", "chunked");
        client.writeChars(CharBuffer.wrap("\r\n"), utf8Encoder);

        return new ChunkedOctetStreamWriterImpl(client, utf8Encoder, state);
    }

    @Override
    public void bodyFromChannel(ReadableByteChannel inChannel, long offset, long numBytes)
            throws IOException, IllegalBuilderStateException {

        if (!state.inHeaders())
            throw new IllegalBuilderStateException(BuilderState.State.Headers, state);

        completeHeaders();
        writeHeader("Content-Length", String.valueOf(numBytes));
        client.writeChars(CharBuffer.wrap("\r\n"), utf8Encoder);

        final ByteBuffer byteBuffer = client.getBuffers().getByteBuffer();
        byteBuffer.clear();

        while (inChannel.read(byteBuffer) > 0) {
            byteBuffer.flip();
            client.getChannel().write(byteBuffer);
            byteBuffer.flip();
        }

        state.complete();
    }

    @Override
    public void bodyFromChannel(ReadableByteChannel inChannel) throws IOException, IllegalBuilderStateException {
        if (!state.inHeaders())
            throw new IllegalBuilderStateException(BuilderState.State.Headers, state);

        final ByteBuffer byteBuffer = client.getBuffers().getByteBuffer();
        byteBuffer.clear();

        final ArrayList<ByteBuffer> buffers = new ArrayList<>();
        int numBytes = 0;

        while (inChannel.read(byteBuffer) > 0) {
            byteBuffer.flip();

            numBytes += byteBuffer.limit();
            final ByteBuffer tmpBuffer = ByteBuffer.allocate(byteBuffer.limit());
            tmpBuffer.put(byteBuffer);

            tmpBuffer.flip();
            byteBuffer.flip();

            buffers.add(tmpBuffer);
        }

        completeHeaders();
        writeHeader("Content-Length", String.valueOf(numBytes));
        client.writeChars(CharBuffer.wrap("\r\n"), utf8Encoder);

        for (final ByteBuffer buffer : buffers)
            client.getChannel().write(buffer);

        state.complete();
    }

    @Override
    public void bodyFromFile(FileChannel inChannel, long offset, long numBytes)
            throws IOException, IllegalBuilderStateException {

        if (!state.inHeaders())
            throw new IllegalBuilderStateException(BuilderState.State.Headers, state);

        completeHeaders();
        writeHeader("Content-Length", String.valueOf(numBytes));
        client.writeChars(CharBuffer.wrap("\r\n"), utf8Encoder);

        inChannel.transferTo(offset, numBytes, client.getChannel());

        state.complete();
    }

    @Override
    public void emptyBody() throws IOException, IllegalBuilderStateException {
        if (!state.inHeaders())
            throw new IllegalBuilderStateException(BuilderState.State.Headers, state);

        completeHeaders();
        client.writeChars(CharBuffer.wrap("\r\n"), utf8Encoder);

        state.complete();
    }

    private void completeHeaders() throws IOException, IllegalBuilderStateException {
        for (final Map.Entry<String, String> entry : injectedHeaders.entrySet())
            writeHeader(entry.getKey(), entry.getValue());

        injectedHeaders.clear();
    }
}

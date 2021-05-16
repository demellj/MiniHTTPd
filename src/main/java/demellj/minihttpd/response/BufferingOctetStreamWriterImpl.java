package demellj.minihttpd.response;

import demellj.minihttpd.Client;
import demellj.minihttpd.OctetStreamWriter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;

public class BufferingOctetStreamWriterImpl implements OctetStreamWriter {
    private final ArrayList<ByteBuffer> buffers = new ArrayList<>();

    final private Client client;
    final private CharsetEncoder utf8Encoder;
    final private BuilderState state;

    public BufferingOctetStreamWriterImpl(Client client,
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

        final ByteBuffer bufferCopy = ByteBuffer.allocate(buffer.limit());
        bufferCopy.put(buffer);
        bufferCopy.flip();

        buffers.add(bufferCopy);
        return this;
    }

    @Override
    public OctetStreamWriter write(byte[] data) throws IOException, IllegalBuilderStateException {
        if (!state.inBody())
            throw new IllegalBuilderStateException(BuilderState.State.Body, state);

        buffers.add(ByteBuffer.wrap(data));
        return this;
    }

    @Override
    public OctetStreamWriter write(byte data) throws IOException, IllegalBuilderStateException {
        if (!state.inBody())
            throw new IllegalBuilderStateException(BuilderState.State.Body, state);

        final byte[] buffer = new byte[]{data};
        buffers.add(ByteBuffer.wrap(buffer));

        return this;
    }

    @Override
    public void complete() throws IOException, IllegalBuilderStateException {
        if (!state.inBody())
            throw new IllegalBuilderStateException(BuilderState.State.Body, state);

        final int length = buffers.stream().reduce(0, (acc, buff) -> buff.remaining() + acc, Integer::sum);

        final StringBuilder headerBuilder = new StringBuilder();
        headerBuilder.append("Content-Length: ")
                     .append(length)
                     .append("\r\n\r\n");

        client.writeChars(CharBuffer.wrap(headerBuilder), utf8Encoder);

        final SocketChannel channel = client.getChannel();
        for (final ByteBuffer buffer : buffers)
            channel.write(buffer);

        state.complete();
    }
}

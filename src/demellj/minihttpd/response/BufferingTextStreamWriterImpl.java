package demellj.minihttpd.response;

import demellj.minihttpd.Client;
import demellj.minihttpd.TextStreamWriter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

public class BufferingTextStreamWriterImpl implements TextStreamWriter {
    final private Client client;
    final private CharsetEncoder utf8Encoder;
    final private BuilderState   state;
    final private CharsetEncoder bodyEncoder;
    final private StringBuilder  builder = new StringBuilder();

    public BufferingTextStreamWriterImpl(Client client,
                                         CharsetEncoder ut8Encoder,
                                         BuilderState state,
                                         Charset bodyCharset) {
        this.client = client;
        this.utf8Encoder = ut8Encoder;
        this.state = state;
        bodyEncoder = bodyCharset.newEncoder();
    }

    @Override
    public TextStreamWriter write(CharBuffer buffer) throws IOException, IllegalBuilderStateException {
        if (!state.inBody())
            throw new IllegalBuilderStateException(BuilderState.State.Body, state);

        if (!state.inBody()) return null;
        builder.append(buffer);
        return this;
    }

    @Override
    public TextStreamWriter write(CharSequence data) throws IOException, IllegalBuilderStateException {
        if (!state.inBody())
            throw new IllegalBuilderStateException(BuilderState.State.Body, state);

        if (!state.inBody()) return null;
        builder.append(data);
        return this;
    }

    @Override
    public TextStreamWriter write(char[] data) throws IOException, IllegalBuilderStateException {
        if (!state.inBody())
            throw new IllegalBuilderStateException(BuilderState.State.Body, state);

        builder.append(data);
        return this;
    }

    @Override
    public TextStreamWriter write(char data) throws IOException, IllegalBuilderStateException {
        if (!state.inBody())
            throw new IllegalBuilderStateException(BuilderState.State.Body, state);

        builder.append(data);
        return this;
    }

    @Override
    public void complete() throws IOException, IllegalBuilderStateException {
        if (!state.inBody())
            throw new IllegalBuilderStateException(BuilderState.State.Body, state);

        final String result = builder.toString();
        builder.delete(0, builder.length() - 1);

        final byte[] data = result.getBytes(bodyEncoder.charset());

        final StringBuilder headerBuilder = new StringBuilder();
        headerBuilder.append("Content-Length: ")
                     .append(data.length)
                     .append("\r\n\r\n");

        client.writeChars(CharBuffer.wrap(headerBuilder), utf8Encoder);

        client.getChannel().write(ByteBuffer.wrap(data));

        state.complete();
    }
}

package demellj.minihttpd.response;

import demellj.minihttpd.Client;
import demellj.minihttpd.TextStreamWriter;

import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

public class TextStreamWriterImpl implements TextStreamWriter {
    final private CharsetEncoder encoder;
    final private Client client;
    final private BuilderState state;

    public TextStreamWriterImpl(Client client, BuilderState state, Charset charset) {
        this.client = client;
        this.state = state;
        encoder = charset.newEncoder();
    }

    @Override
    public TextStreamWriter write(CharBuffer buffer) throws IOException, IllegalBuilderStateException {
        if (!state.inBody())
            throw new IllegalBuilderStateException(BuilderState.State.Body, state);

        client.writeChars(buffer, encoder);
        return this;
    }

    @Override
    public TextStreamWriter write(CharSequence data) throws IOException, IllegalBuilderStateException {
        if (!state.inBody())
            throw new IllegalBuilderStateException(BuilderState.State.Body, state);

        client.writeChars(CharBuffer.wrap(data), encoder);
        return this;
    }

    @Override
    public TextStreamWriter write(char[] data) throws IOException, IllegalBuilderStateException {
        if (!state.inBody())
            throw new IllegalBuilderStateException(BuilderState.State.Body, state);

        client.writeChars(CharBuffer.wrap(data), encoder);
        return this;
    }

    @Override
    public TextStreamWriter write(char data) throws IOException, IllegalBuilderStateException {
        if (!state.inBody())
            throw new IllegalBuilderStateException(BuilderState.State.Body, state);

        client.writeChars(CharBuffer.wrap(String.valueOf(data)), encoder);
        return this;
    }

    @Override
    public void complete() throws IllegalBuilderStateException {
        if (!state.inBody())
            throw new IllegalBuilderStateException(BuilderState.State.Body, state);

        state.complete();
    }
}

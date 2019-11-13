package demellj.minihttpd.response;

import demellj.minihttpd.Client;
import demellj.minihttpd.OctetStreamWriter;

import java.io.IOException;
import java.nio.ByteBuffer;

public class OctetStreamWriterImpl implements OctetStreamWriter {
    final private Client client;
    final private BuilderState state;

    public OctetStreamWriterImpl(Client client, BuilderState state) {
        this.client = client;
        this.state = state;
    }

    @Override
    public OctetStreamWriter write(ByteBuffer buffer) throws IOException, IllegalBuilderStateException {
        if (!state.inBody())
            throw new IllegalBuilderStateException(BuilderState.State.Body, state);

        client.getChannel().write(buffer);
        return this;
    }

    @Override
    public OctetStreamWriter write(byte[] data) throws IOException, IllegalBuilderStateException {
        if (!state.inBody())
            throw new IllegalBuilderStateException(BuilderState.State.Body, state);

        write(ByteBuffer.wrap(data));
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

        client.getChannel().write(byteBuffer);

        return this;
    }

    @Override
    public void complete() throws IllegalBuilderStateException {
        if (!state.inBody())
            throw new IllegalBuilderStateException(BuilderState.State.Body, state);

        state.complete();
    }
}

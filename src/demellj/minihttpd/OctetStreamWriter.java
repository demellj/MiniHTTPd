package demellj.minihttpd;

import demellj.minihttpd.response.IllegalBuilderStateException;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface OctetStreamWriter {
    OctetStreamWriter write(ByteBuffer buffer) throws IOException, IllegalBuilderStateException;

    OctetStreamWriter write(byte[] data) throws IOException, IllegalBuilderStateException;

    OctetStreamWriter write(byte data) throws IOException, IllegalBuilderStateException;

    void complete() throws IOException, IllegalBuilderStateException;
}

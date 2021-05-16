package demellj.minihttpd;

import demellj.minihttpd.response.IllegalBuilderStateException;

import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;

public interface HeaderWriter {
    HeaderWriter writeContentType(String format) throws IOException, IllegalBuilderStateException;

    HeaderWriter writeContentLength(long numBytes) throws IOException, IllegalBuilderStateException;

    HeaderWriter writeHeader(CharSequence key, CharSequence value) throws IOException, IllegalBuilderStateException;

    HeaderWriter writeHeader(CharSequence key, CharBuffer valueBuffer)
            throws IOException, IllegalBuilderStateException;

    TextStreamWriter textBody(Charset charset, long numBytes) throws IOException, IllegalBuilderStateException;

    TextStreamWriter textBody(Charset charset) throws IOException, IllegalBuilderStateException;

    TextStreamWriter textBodyChunked(Charset charset) throws IOException, IllegalBuilderStateException;

    OctetStreamWriter octetStreamBody(long numBytes) throws IOException, IllegalBuilderStateException;

    OctetStreamWriter octetStreamBody() throws IOException, IllegalBuilderStateException;

    OctetStreamWriter octetStreamBodyChunked() throws IOException, IllegalBuilderStateException;

    void bodyFromChannel(ReadableByteChannel inChannel, long offset, long numBytes)
            throws IOException, IllegalBuilderStateException;

    void bodyFromChannel(ReadableByteChannel inChannel) throws IOException, IllegalBuilderStateException;

    void bodyFromFile(FileChannel inChannel, long offset, long numBytes)
            throws IOException, IllegalBuilderStateException;

    void emptyBody() throws IOException, IllegalBuilderStateException;
}

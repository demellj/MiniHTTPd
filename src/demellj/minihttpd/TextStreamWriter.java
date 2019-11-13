package demellj.minihttpd;

import demellj.minihttpd.response.IllegalBuilderStateException;

import java.io.IOException;
import java.nio.CharBuffer;

public interface TextStreamWriter {
    TextStreamWriter write(CharBuffer buffer) throws IOException, IllegalBuilderStateException;

    TextStreamWriter write(CharSequence data) throws IOException, IllegalBuilderStateException;

    TextStreamWriter write(char[] data) throws IOException, IllegalBuilderStateException;

    TextStreamWriter write(char data) throws IOException, IllegalBuilderStateException;

    void complete() throws IOException, IllegalBuilderStateException;
}

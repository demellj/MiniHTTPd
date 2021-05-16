package demellj.minihttpd;

import demellj.minihttpd.response.BuilderState;
import demellj.minihttpd.response.HeaderWriterImpl;
import demellj.minihttpd.response.IllegalBuilderStateException;

import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.TreeMap;

public class ResponseWriter {
    final private Client client;
    final private String protocol;

    final private CharsetEncoder utf8Encoder = StandardCharsets.UTF_8.newEncoder();
    final private TreeMap<String, String> injectedHeaders = new TreeMap<>();

    private final BuilderState state = new BuilderState();

    ResponseWriter(String protocol, Client client) {
        this.protocol = protocol;
        this.client = client;
    }

    void appendHeader(String key, String value) {
        injectedHeaders.put(key, value);
    }

    public HeaderWriter begin(HTTPStatus status) throws IOException, IllegalBuilderStateException {
        if (!state.inBegin())
            throw new IllegalBuilderStateException(BuilderState.State.Begin, state);

        final StringBuilder builder = new StringBuilder();
        builder.append(protocol)
               .append(' ')
               .append(status.statusLine)
               .append("\r\n");

        client.writeChars(CharBuffer.wrap(builder), utf8Encoder);

        return new HeaderWriterImpl(client, state, injectedHeaders, utf8Encoder);
    }
}
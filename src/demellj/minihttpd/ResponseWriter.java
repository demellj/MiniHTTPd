package demellj.minihttpd;

import demellj.minihttpd.response.*;

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
        this.client   = client;
    }

    void appendHeader(String key, String value) {
        injectedHeaders.put(key, value);
    }

    public HeaderWriter begin(HTTPStatus status) throws IOException {
        if (!state.inBegin()) return null;

        final StringBuilder builder = new StringBuilder();
        builder.append(protocol)
               .append(' ')
               .append(status.statusLine)
               .append("\r\n");

        client.writeChars(CharBuffer.wrap(builder), utf8Encoder);

        return new HeaderWriterImpl(client, state, injectedHeaders, utf8Encoder);
    }
}
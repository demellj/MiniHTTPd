package demellj.minihttpd;

import java.util.Map;
import java.util.TreeMap;

public class Response {
    private String protocolVersion = "HTTP/1.1";
    private final String statusLine;
    private final Map<String, String> headers = new TreeMap<>();
    private final StringBuilder body = new StringBuilder();

    public Response(String statusLine) {
        this.statusLine = statusLine;
    }

    void setProtocolVersion(String version) {
        this.protocolVersion = version;
    }

    public void putHeader(String key, String value) {
        headers.put(key.trim(), value.trim());
    }

    public StringBuilder buildBody() {
        return body;
    }

    public String getHeaderText() {
        final StringBuilder strb = new StringBuilder();

        strb.append(protocolVersion).append(' ').append(statusLine).append("\r\n");
        for (final var entry : headers.entrySet())
            strb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\r\n");
        strb.append("\r\n");

        return strb.toString();
    }

    public String getBodyText() {
        return body.toString();
    }

    public String getCompleteText() {
        final StringBuilder strb = new StringBuilder();

        strb.append(getHeaderText());
        strb.append(body);

        return strb.toString();
    }

    public static class Factory {
        static Response http100 = new Response("100 Continue");

        static Response http404 = new Response("404 Not found");

        public static Response new100() {
            return http100;
        }

        public static Response new200(final String body) {
            final Response resp = new Response("200 OK");
            resp.putHeader("Content-type", "text/html; charset=UTF-8");
            resp.buildBody().append(body);
            return resp;
        }

        public static Response new404() {
            return http404;
        }
    }
}

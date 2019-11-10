package demellj.minihttpd;

import java.nio.charset.StandardCharsets;
import java.util.*;


public class Request {
    public enum Type {
        GET, POST;

        public static Type fromString(String s) {
            final String ls = s.toUpperCase();
            if (ls.equals("GET"))
                return GET;
            if (ls.equals("POST"))
                return POST;
            return null;
        }

        public String toString() {
            switch (this) {
                case GET:
                    return "GET";
                case POST:
                    return "POST";
            }
            return null;
        }
    }

    public final Type type;
    public final String path;
    public final Map<String, String> headers;
    public final String query;
    public final Map<String, String> urlparams;

    public Request(Type type, String path, Map<String, String> headers) {
        this.type = type;
        this.path = path;
        this.headers = headers;

        this.urlparams = new TreeMap<String, String>();

        final int qidx = path.indexOf("?");
        if (qidx > -1) {
            this.query = path.substring(qidx + 1);

            for (final String param : this.query.split("&")) {
                final int pidx = param.indexOf("=");

                if (pidx >= 0 && pidx < param.length()) {
                    try {
                        final String key = java.net.URLDecoder.decode(param.substring(0, pidx), StandardCharsets.UTF_8);
                        final String val = java.net.URLDecoder.decode(param.substring(pidx + 1), StandardCharsets.UTF_8);

                        this.urlparams.put(key, val);
                    } catch (Exception e) {
                    }
                }
            }
        } else {
            this.query = "";
        }
    }

    public static class Parser {
        public static Request parse(LineBuffer buffer) {
            if (hasCompleteMessage(buffer)) {
                String line = buffer.nextLine();

                if (line == null || line.isEmpty()) return null;

                String[] words = line.split(" +");

                if (words.length < 2) return null;

                final Type type = Type.fromString(words[0]);
                final String path = words[1];

                Map<String, String> opts = new TreeMap<>();

                while ((line = buffer.nextLine()) != null && !line.isEmpty()) {
                    final int idx = line.indexOf(":");

                    if (idx >= 0) {
                        opts.put(line.substring(0, idx).trim().toLowerCase(),
                                line.substring(idx + 1).trim());
                    }
                }

                return new Request(type, path, opts);
            }

            return null;
        }

        // Search for an empty line terminated with: \r\n
        private static boolean hasCompleteMessage(LineBuffer buffer) {
            for (final String line : buffer) {
                if (line.isEmpty()) return true;
            }

            return false;
        }
    }
}

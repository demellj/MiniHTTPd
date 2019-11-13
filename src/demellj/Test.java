package demellj;

import demellj.minihttpd.HTTPStatus;
import demellj.minihttpd.MiniHTTPd;
import demellj.minihttpd.response.IllegalBuilderStateException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Test {
    public static void main(String[] args) throws Exception {
        final MiniHTTPd server = new MiniHTTPd(9000);

        final String cwd = new File(".").getCanonicalPath();

        server.setResponder((req, writer) -> {
            try {
                if (req.path.equals("/favicon.ico")) {
                    writer.begin(HTTPStatus.HTTP_404)
                          .emptyBody();
                } else {
                    writer.begin(HTTPStatus.HTTP_200)
                          .writeContentType("text/html; charset=UTF-8")
                          .textBodyChunked(StandardCharsets.UTF_8)
                          .write("<b>path:</b> ")
                          .write(req.path)
                          .write("</br><b>type:</b> ")
                          .write(req.type.toString())
                          .write("</br>")
                          .write(req.headers.toString())
                          .complete();
                }
            } catch (IOException | IllegalBuilderStateException e) {
                e.printStackTrace();
            }
        });

        server.startup(4);
    }
}

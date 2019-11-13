package demellj.minihttpd;

public enum HTTPStatus {
    HTTP_100("100 Continue"),
    HTTP_200("200 OK"),
    HTTP_404("404 Not found");

    final String statusLine;

    HTTPStatus(String statusLine) {
        this.statusLine = statusLine;
    }
}

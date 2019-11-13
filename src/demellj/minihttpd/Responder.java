package demellj.minihttpd;

public interface Responder {
    void respond(Request req, ResponseWriter writer);
}

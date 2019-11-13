package demellj.minihttpd.response;

public class BuilderState {
    public enum State { Begin, Headers, Body, Complete}

    private State state = State.Begin;

    public boolean inBegin() {
        return state.compareTo(State.Begin) == 0;
    }

    public boolean inHeaders() {
        if (state.compareTo(State.Headers) <= 0) {
            state = State.Headers;
            return true;
        }
        return false;
    }

    public boolean inBody() {
        if (state.compareTo(State.Body) <= 0) {
            state = State.Body;
            return true;
        }
        return false;
    }

    public boolean complete() {
        state = State.Complete;
        return true;
    }

    @Override
    public String toString() {
        return state.name();
    }
}

package demellj.minihttpd.response;

public class IllegalBuilderStateException extends Exception {
    public IllegalBuilderStateException(BuilderState.State expected, BuilderState found) {
        super(String.format("Attempting to perform '%s' operation(s) while having transitioned to %s state",
                expected, found));
    }
}

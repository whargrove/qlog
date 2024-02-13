package qlog.exc;

public class TailReaderFileNotFoundException extends TailReaderException {
    public TailReaderFileNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

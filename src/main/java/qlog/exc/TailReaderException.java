package qlog.exc;

public class TailReaderException extends RuntimeException {
    public TailReaderException(String message, Throwable cause) {
        super(message, cause);
    }
}

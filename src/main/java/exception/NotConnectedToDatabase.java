package exception;

public class NotConnectedToDatabase extends RuntimeException {

    public NotConnectedToDatabase(String message) {
        super(message);
    }

}

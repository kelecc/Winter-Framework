package top.kelecc.winter.exception;

/**
 * @author 可乐
 */
public class TransactionException extends DataAccessException {

    public TransactionException() {
    }

    public TransactionException(String message) {
        super(message);
    }

    public TransactionException(Throwable cause) {
        super(cause);
    }

    public TransactionException(String message, Throwable cause) {
        super(message, cause);
    }

}

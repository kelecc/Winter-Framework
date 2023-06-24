package top.kelecc.winter.exception;

/**
 * @author 可乐
 * @version 1.0
 * @description:
 * @date 2023/6/23 23:19
 */
public class DataAccessException extends NestedRuntimeException {
    public DataAccessException() {
        super();
    }

    public DataAccessException(String message) {
        super(message);
    }

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataAccessException(Throwable cause) {
        super(cause);
    }
}

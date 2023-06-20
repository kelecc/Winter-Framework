package top.kelecc.exception;

/**
 * @author 可乐
 * @version 1.0
 * @description:
 * @date 2023/6/19 15:40
 */
public class NestedRuntimeException extends RuntimeException{
    public NestedRuntimeException() {

    }

    public NestedRuntimeException(String message) {
        super(message);
    }

    public NestedRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public NestedRuntimeException(Throwable cause) {
        super(cause);
    }
}

package top.kelecc.exception;

/**
 * @author 可乐
 * @version 1.0
 * @description:
 * @date 2023/6/19 15:39
 */
public class BeansException extends NestedRuntimeException{
    public BeansException() {
    }

    public BeansException(String message) {
        super(message);
    }

    public BeansException(String message, Throwable cause) {
        super(message, cause);
    }

    public BeansException(Throwable cause) {
        super(cause);
    }
}

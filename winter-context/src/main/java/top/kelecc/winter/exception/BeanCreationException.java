package top.kelecc.winter.exception;

/**
 * @author 可乐
 * @version 1.0
 * @description:
 * @date 2023/6/19 16:03
 */
public class BeanCreationException extends BeansException {
    public BeanCreationException() {
    }

    public BeanCreationException(String message) {
        super(message);
    }

    public BeanCreationException(String message, Throwable cause) {
        super(message, cause);
    }

    public BeanCreationException(Throwable cause) {
        super(cause);
    }
}

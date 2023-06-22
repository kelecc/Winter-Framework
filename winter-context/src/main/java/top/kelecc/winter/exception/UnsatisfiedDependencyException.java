package top.kelecc.winter.exception;

/**
 * @author 可乐
 * @version 1.0
 * @description:
 * @date 2023/6/20 17:17
 */
public class UnsatisfiedDependencyException extends BeanCreationException {
    public UnsatisfiedDependencyException() {
    }

    public UnsatisfiedDependencyException(String message) {
        super(message);
    }

    public UnsatisfiedDependencyException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnsatisfiedDependencyException(Throwable cause) {
        super(cause);
    }
}

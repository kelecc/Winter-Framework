package top.kelecc.winter.exception;

/**
 * @author 可乐
 * @version 1.0
 * @description:
 * @date 2023/6/22 20:38
 */
public class AopConfigException extends RuntimeException {
    public AopConfigException() {
        super();
    }

    public AopConfigException(String message) {
        super(message);
    }

    public AopConfigException(String message, Throwable cause) {
        super(message, cause);
    }

    public AopConfigException(Throwable cause) {
        super(cause);
    }
}
